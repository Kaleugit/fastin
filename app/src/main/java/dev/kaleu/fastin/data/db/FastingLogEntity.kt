package dev.kaleu.fastin.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import dev.kaleu.fastin.domain.model.FastingLog
import dev.kaleu.fastin.domain.model.Quality
import dev.kaleu.fastin.domain.model.Tristate
import dev.kaleu.fastin.domain.model.YesNo
import java.time.LocalDate
import java.time.LocalTime

/**
 * Uma linha por dia. `date` é a PK — é o que faz o salvar ser upsert natural.
 * Datas e horas gravadas como TEXT ISO-8601 (ADR-004): legível no CSV e ordenável no SQL.
 */
@Entity(tableName = "fasting_log")
data class FastingLogEntity(
    @PrimaryKey
    @ColumnInfo(name = "date") val date: String,
    @ColumnInfo(name = "last_meal_time") val lastMealTime: String? = null,
    @ColumnInfo(name = "first_meal_time") val firstMealTime: String? = null,
    @ColumnInfo(name = "caloric_deficit") val caloricDeficit: String? = null,
    @ColumnInfo(name = "meal_quality") val mealQuality: String? = null,
    @ColumnInfo(name = "water_2l") val water2l: String? = null,
    @ColumnInfo(name = "alcohol") val alcohol: String? = null,
    @ColumnInfo(name = "weight") val weight: Double? = null,
    @ColumnInfo(name = "notes") val notes: String? = null,
)

fun FastingLogEntity.toDomain(): FastingLog = FastingLog(
    date = LocalDate.parse(date),
    lastMealTime = lastMealTime?.let(LocalTime::parse),
    firstMealTime = firstMealTime?.let(LocalTime::parse),
    caloricDeficit = caloricDeficit?.let { enumValueOf<Tristate>(it) },
    mealQuality = mealQuality?.let { enumValueOf<Quality>(it) },
    water2l = water2l?.let { enumValueOf<Tristate>(it) },
    alcohol = alcohol?.let { enumValueOf<YesNo>(it) },
    weight = weight,
    notes = notes,
)

fun FastingLog.toEntity(): FastingLogEntity = FastingLogEntity(
    date = date.toString(),
    lastMealTime = lastMealTime?.toString(),
    firstMealTime = firstMealTime?.toString(),
    caloricDeficit = caloricDeficit?.name,
    mealQuality = mealQuality?.name,
    water2l = water2l?.name,
    alcohol = alcohol?.name,
    weight = weight,
    notes = notes?.takeIf { it.isNotBlank() },
)
