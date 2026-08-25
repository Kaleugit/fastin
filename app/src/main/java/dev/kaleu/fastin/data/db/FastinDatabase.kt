package dev.kaleu.fastin.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [FastingLogEntity::class], version = 1, exportSchema = true)
abstract class FastinDatabase : RoomDatabase() {

    abstract fun fastingLogDao(): FastingLogDao

    companion object {
        private const val NAME = "fastin.db"

        @Volatile
        private var instance: FastinDatabase? = null

        fun get(context: Context): FastinDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    FastinDatabase::class.java,
                    NAME,
                ).build().also { instance = it }
            }
    }
}
