package dev.kaleu.fastin.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface FastingLogDao {

    @Query("SELECT * FROM fasting_log ORDER BY date ASC")
    fun observeAll(): Flow<List<FastingLogEntity>>

    /**
     * Intervalo inclusivo. A comparação lexicográfica de TEXT funciona porque as datas são
     * ISO-8601 de largura fixa.
     */
    @Query("SELECT * FROM fasting_log WHERE date BETWEEN :from AND :to ORDER BY date ASC")
    fun observeRange(from: String, to: String): Flow<List<FastingLogEntity>>

    @Query("SELECT * FROM fasting_log WHERE date = :date")
    fun observeByDate(date: String): Flow<FastingLogEntity?>

    @Query("SELECT * FROM fasting_log WHERE date = :date")
    suspend fun getByDate(date: String): FastingLogEntity?

    @Upsert
    suspend fun upsert(entity: FastingLogEntity)

    @Upsert
    suspend fun upsertAll(entities: List<FastingLogEntity>)

    @Delete
    suspend fun delete(entity: FastingLogEntity)

    @Query("DELETE FROM fasting_log WHERE date = :date")
    suspend fun deleteByDate(date: String)
}
