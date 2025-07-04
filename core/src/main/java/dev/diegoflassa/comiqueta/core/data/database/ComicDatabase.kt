package dev.diegoflassa.comiqueta.core.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import dev.diegoflassa.comiqueta.core.data.database.dao.ComicDao
import dev.diegoflassa.comiqueta.core.data.database.entity.ComicEntity
import dev.diegoflassa.comiqueta.core.data.extensions.modoDebugHabilitado

@Database(entities = [ComicEntity::class], version = 1, exportSchema = false)
abstract class ComicDatabase : RoomDatabase() {

    abstract fun comicDao(): ComicDao

    companion object {
        @Volatile
        private var INSTANCE: ComicDatabase? = null

        fun getDatabase(context: Context): ComicDatabase {
            return INSTANCE ?: synchronized(this) {
                val builder = Room.databaseBuilder(
                    context.applicationContext,
                    ComicDatabase::class.java,
                    "comiqueta_database"
                )
                if (context.modoDebugHabilitado()) {
                    builder.fallbackToDestructiveMigration(true)
                }
                val instance = builder.build()
                INSTANCE = instance
                instance
            }
        }
    }
}