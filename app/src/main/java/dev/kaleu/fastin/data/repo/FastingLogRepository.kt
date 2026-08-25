package dev.kaleu.fastin.data.repo

import dev.kaleu.fastin.data.db.FastingLogDao
import dev.kaleu.fastin.data.db.toDomain
import dev.kaleu.fastin.data.db.toEntity
import dev.kaleu.fastin.domain.model.FastingLog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

/**
 * Única fronteira entre persistência e o resto do app. Expõe modelos de domínio; nenhum
 * ViewModel toca DAO ou entity.
 */
class FastingLogRepository(private val dao: FastingLogDao) {

    fun observeAll(): Flow<List<FastingLog>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    /** Indexado por data — formato que [FastingCalculator] consome direto. */
    fun observeAllByDate(): Flow<Map<LocalDate, FastingLog>> =
        observeAll().map { list -> list.associateBy { it.date } }

    fun observeRange(from: LocalDate, to: LocalDate): Flow<List<FastingLog>> =
        dao.observeRange(from.toString(), to.toString()).map { list -> list.map { it.toDomain() } }

    fun observeByDate(date: LocalDate): Flow<FastingLog?> =
        dao.observeByDate(date.toString()).map { it?.toDomain() }

    suspend fun get(date: LocalDate): FastingLog? = dao.getByDate(date.toString())?.toDomain()

    /**
     * Salvar um dia esvaziado apaga a linha em vez de gravar uma cheia de nulos — assim o
     * calendário não mostra ponto em dia sem dado.
     */
    suspend fun save(log: FastingLog) {
        if (log.isEmpty) dao.deleteByDate(log.date.toString()) else dao.upsert(log.toEntity())
    }

    suspend fun saveAll(logs: List<FastingLog>) =
        dao.upsertAll(logs.filterNot { it.isEmpty }.map { it.toEntity() })

    suspend fun delete(date: LocalDate) = dao.deleteByDate(date.toString())
}
