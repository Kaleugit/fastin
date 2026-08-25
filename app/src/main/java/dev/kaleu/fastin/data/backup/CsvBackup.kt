package dev.kaleu.fastin.data.backup

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import dev.kaleu.fastin.data.repo.FastingLogRepository
import dev.kaleu.fastin.domain.model.FastingLog
import dev.kaleu.fastin.domain.model.Quality
import dev.kaleu.fastin.domain.model.Tristate
import dev.kaleu.fastin.domain.model.YesNo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate
import java.time.LocalTime

/**
 * Backup manual em CSV (PROJECT.md §4.2, ADR-006).
 *
 * É o **único** backup existente: o app é sideload, não tem nuvem e não tem conta. Por isso
 * export e import são estritamente simétricos — o arquivo exportado tem que reimportar sem
 * perda, senão o backup é decorativo.
 */
class CsvBackup(
    private val context: Context,
    private val repository: FastingLogRepository,
) {

    data class ImportResult(
        val imported: Int,
        val skipped: Int,
        val errors: List<String>,
    )

    /**
     * Exporta para a pasta Downloads. Retorna o nome do arquivo criado.
     *
     * Em API 29+ usa MediaStore (sem permissão de escrita); abaixo disso escreve direto no
     * diretório público, que ainda é permitido.
     */
    suspend fun exportToDownloads(today: LocalDate): String = withContext(Dispatchers.IO) {
        val logs = repository.observeAll().first().sortedBy { it.date }
        val content = toCsv(logs)
        val name = "fastin-backup-$today.csv"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val uri = context.contentResolver
                .insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: error("não consegui criar o arquivo em Downloads")
            context.contentResolver.openOutputStream(uri)?.use { it.write(content.toByteArray()) }
                ?: error("não consegui escrever em $name")
        } else {
            @Suppress("DEPRECATION")
            val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            dir.mkdirs()
            File(dir, name).writeText(content)
        }
        name
    }

    suspend fun importFrom(uri: Uri): ImportResult = withContext(Dispatchers.IO) {
        val text = context.contentResolver.openInputStream(uri)?.use {
            it.readBytes().toString(Charsets.UTF_8)
        } ?: return@withContext ImportResult(0, 0, listOf("não consegui abrir o arquivo"))
        importFromText(text)
    }

    suspend fun importFromText(text: String): ImportResult {
        val parsed = parseCsv(text)
        if (parsed.logs.isNotEmpty()) repository.saveAll(parsed.logs)
        return ImportResult(
            imported = parsed.logs.size,
            skipped = parsed.skipped,
            errors = parsed.errors,
        )
    }

    // ---------------------------------------------------------------- serialização

    fun toCsv(logs: List<FastingLog>): String = buildString {
        appendLine(HEADER.joinToString(","))
        logs.forEach { log ->
            appendLine(
                listOf(
                    log.date.toString(),
                    log.lastMealTime?.toString().orEmpty(),
                    log.firstMealTime?.toString().orEmpty(),
                    log.caloricDeficit?.name.orEmpty(),
                    log.mealQuality?.name.orEmpty(),
                    log.water2l?.name.orEmpty(),
                    log.alcohol?.name.orEmpty(),
                    // Ponto como separador decimal: vírgula quebraria o CSV.
                    log.weight?.toString().orEmpty(),
                    escape(log.notes.orEmpty()),
                ).joinToString(","),
            )
        }
    }

    private data class Parsed(
        val logs: List<FastingLog>,
        val skipped: Int,
        val errors: List<String>,
    )

    private fun parseCsv(text: String): Parsed {
        val lines = text.lineSequence().filter { it.isNotBlank() }.toList()
        if (lines.isEmpty()) return Parsed(emptyList(), 0, listOf("arquivo vazio"))

        // Header opcional: um CSV editado à mão pode tê-lo perdido.
        val body = if (lines.first().startsWith(HEADER.first())) lines.drop(1) else lines

        val logs = mutableListOf<FastingLog>()
        val errors = mutableListOf<String>()
        var skipped = 0

        body.forEachIndexed { index, line ->
            val cells = splitCsvLine(line)
            if (cells.isEmpty() || cells[0].isBlank()) {
                skipped++
                return@forEachIndexed
            }
            val date = runCatching { LocalDate.parse(cells[0].trim()) }.getOrNull()
            if (date == null) {
                skipped++
                // Linha 1 do corpo é a linha 2 do arquivo quando há header.
                if (errors.size < MAX_REPORTED_ERRORS) {
                    errors += "linha ${index + 2}: data inválida '${cells[0]}'"
                }
                return@forEachIndexed
            }

            logs += FastingLog(
                date = date,
                lastMealTime = cells.getOrNull(1).parseTime(),
                firstMealTime = cells.getOrNull(2).parseTime(),
                caloricDeficit = cells.getOrNull(3).parseEnum<Tristate>(),
                mealQuality = cells.getOrNull(4).parseEnum<Quality>(),
                water2l = cells.getOrNull(5).parseEnum<Tristate>(),
                alcohol = cells.getOrNull(6).parseEnum<YesNo>(),
                // Aceita vírgula: um CSV editado no Excel pt-BR pode trazê-la.
                weight = cells.getOrNull(7)?.trim()?.replace(',', '.')?.toDoubleOrNull(),
                notes = cells.getOrNull(8)?.takeIf { it.isNotBlank() },
            )
        }
        return Parsed(logs, skipped, errors)
    }

    private fun String?.parseTime(): LocalTime? =
        this?.trim()?.takeIf { it.isNotEmpty() }?.let { runCatching { LocalTime.parse(it) }.getOrNull() }

    private inline fun <reified T : Enum<T>> String?.parseEnum(): T? =
        this?.trim()?.takeIf { it.isNotEmpty() }
            ?.let { runCatching { enumValueOf<T>(it.uppercase()) }.getOrNull() }

    /** Aspas duplas e vírgula em `notes` precisam sobreviver ao round-trip. */
    private fun escape(value: String): String =
        if (value.contains(',') || value.contains('"') || value.contains('\n')) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }

    /** Parser de linha que respeita campo entre aspas — `split(",")` corromperia `notes`. */
    private fun splitCsvLine(line: String): List<String> {
        val cells = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                c == '"' && inQuotes && i + 1 < line.length && line[i + 1] == '"' -> {
                    current.append('"'); i++
                }
                c == '"' -> inQuotes = !inQuotes
                c == ',' && !inQuotes -> {
                    cells += current.toString(); current.clear()
                }
                else -> current.append(c)
            }
            i++
        }
        cells += current.toString()
        return cells
    }

    companion object {
        private const val MAX_REPORTED_ERRORS = 5

        val HEADER = listOf(
            "date",
            "last_meal_time",
            "first_meal_time",
            "caloric_deficit",
            "meal_quality",
            "water_2l",
            "alcohol",
            "weight",
            "notes",
        )
    }
}
