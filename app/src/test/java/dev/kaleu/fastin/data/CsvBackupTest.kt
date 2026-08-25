package dev.kaleu.fastin.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import dev.kaleu.fastin.data.backup.CsvBackup
import dev.kaleu.fastin.data.db.FastinDatabase
import dev.kaleu.fastin.data.repo.FastingLogRepository
import dev.kaleu.fastin.domain.model.FastingLog
import dev.kaleu.fastin.domain.model.Quality
import dev.kaleu.fastin.domain.model.Tristate
import dev.kaleu.fastin.domain.model.YesNo
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate
import java.time.LocalTime

/**
 * Backup CSV (ADR-006).
 *
 * O invariante que define esta feature: **exportar e reimportar não pode perder nada.** É o
 * único backup do app; se o round-trip perder um campo, o usuário perde histórico ao trocar
 * de aparelho e só descobre quando já é tarde.
 */
@RunWith(RobolectricTestRunner::class)
class CsvBackupTest {

    private lateinit var db: FastinDatabase
    private lateinit var repo: FastingLogRepository
    private lateinit var backup: CsvBackup

    @Before fun setUp() {
        val ctx: Context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(ctx, FastinDatabase::class.java)
            .allowMainThreadQueries().build()
        repo = FastingLogRepository(db.fastingLogDao())
        backup = CsvBackup(ctx, repo)
    }

    @After fun tearDown() = db.close()

    private val full = FastingLog(
        date = LocalDate.parse("2026-03-10"),
        lastMealTime = LocalTime.parse("20:30"),
        firstMealTime = LocalTime.parse("12:15"),
        caloricDeficit = Tristate.MAYBE,
        mealQuality = Quality.GOOD,
        water2l = Tristate.YES,
        alcohol = YesNo.NO,
        weight = 82.4,
        notes = "jantar fora",
    )

    @Test
    fun `round-trip completo nao perde nenhum campo`() = runBlocking {
        val csv = backup.toCsv(listOf(full))
        repo.delete(full.date)

        val result = backup.importFromText(csv)
        assertEquals(1, result.imported)
        assertEquals(full, repo.get(full.date))
    }

    @Test
    fun `cabecalho tem as nove colunas do modelo`() {
        val header = backup.toCsv(emptyList()).trim()
        assertEquals(CsvBackup.HEADER.joinToString(","), header)
        assertEquals(9, CsvBackup.HEADER.size)
    }

    /**
     * `notes` com vírgula é o caso que quebra um parser ingênuo de `split(",")` — e é texto
     * livre, então acontece de verdade.
     */
    @Test
    fun `notes com virgula e aspas sobrevivem ao round-trip`() = runBlocking {
        val tricky = full.copy(notes = "jantar fora, com \"amigos\"; comi demais")
        val csv = backup.toCsv(listOf(tricky))

        val result = backup.importFromText(csv)
        assertEquals(1, result.imported)
        assertEquals(tricky.notes, repo.get(tricky.date)!!.notes)
    }

    @Test
    fun `campos vazios voltam como nulos, nao como string vazia`() = runBlocking {
        val sparse = FastingLog(date = LocalDate.parse("2026-03-11"), weight = 80.0)
        val csv = backup.toCsv(listOf(sparse))

        backup.importFromText(csv)
        val stored = repo.get(sparse.date)!!
        assertNull(stored.lastMealTime)
        assertNull(stored.mealQuality)
        assertNull(stored.notes)
        assertEquals(80.0, stored.weight!!, 0.001)
    }

    @Test
    fun `importar e upsert - reimportar nao duplica`() = runBlocking {
        val csv = backup.toCsv(listOf(full))
        backup.importFromText(csv)
        backup.importFromText(csv)
        assertEquals(1, repo.observeAll().first().size)
    }

    @Test
    fun `linha com data invalida e ignorada e reportada, sem derrubar o resto`() = runBlocking {
        val csv = buildString {
            appendLine(CsvBackup.HEADER.joinToString(","))
            appendLine("nao-e-data,20:00,,,,,,,")
            appendLine("2026-03-12,20:00,,,,,,81.0,")
        }
        val result = backup.importFromText(csv)

        assertEquals(1, result.imported)
        assertEquals(1, result.skipped)
        assertTrue(result.errors.first().contains("data inválida"))
        assertEquals(81.0, repo.get(LocalDate.parse("2026-03-12"))!!.weight!!, 0.001)
    }

    @Test
    fun `arquivo sem cabecalho ainda importa`() = runBlocking {
        val result = backup.importFromText("2026-03-12,20:00,,,,,,81.0,\n")
        assertEquals(1, result.imported)
    }

    @Test
    fun `peso com virgula decimal e aceito na importacao`() = runBlocking {
        // Um CSV aberto e salvo no Excel pt-BR pode voltar com vírgula.
        backup.importFromText("2026-03-12,,,,,,,\"81,5\",\n")
        assertEquals(81.5, repo.get(LocalDate.parse("2026-03-12"))!!.weight!!, 0.001)
    }

    @Test
    fun `enum em caixa baixa e aceito`() = runBlocking {
        backup.importFromText("2026-03-12,,,maybe,good,yes,no,,\n")
        val stored = repo.get(LocalDate.parse("2026-03-12"))!!
        assertEquals(Tristate.MAYBE, stored.caloricDeficit)
        assertEquals(Quality.GOOD, stored.mealQuality)
        assertEquals(Tristate.YES, stored.water2l)
        assertEquals(YesNo.NO, stored.alcohol)
    }

    @Test
    fun `arquivo vazio reporta erro em vez de crashar`() = runBlocking {
        val result = backup.importFromText("")
        assertEquals(0, result.imported)
        assertTrue(result.errors.isNotEmpty())
    }

    @Test
    fun `lixo total nao importa nada e nao derruba o app`() = runBlocking {
        val result = backup.importFromText("isso não é um csv\nnem isso\n")
        assertEquals(0, result.imported)
        assertEquals(2, result.skipped)
        assertTrue(repo.observeAll().first().isEmpty())
    }
}