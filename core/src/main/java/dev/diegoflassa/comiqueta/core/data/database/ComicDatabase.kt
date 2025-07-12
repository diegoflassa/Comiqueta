package dev.diegoflassa.comiqueta.core.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import dev.diegoflassa.comiqueta.core.data.database.converters.UriConverter
import dev.diegoflassa.comiqueta.core.data.database.dao.CategoryDao
import dev.diegoflassa.comiqueta.core.data.database.dao.ComicsDao
import dev.diegoflassa.comiqueta.core.data.database.entity.CategoryEntity
import dev.diegoflassa.comiqueta.core.data.database.entity.ComicEntity
import dev.diegoflassa.comiqueta.core.data.extensions.modoDebugHabilitado
import javax.inject.Inject

@TypeConverters(UriConverter::class)
@Database(
    entities = [ComicEntity::class, CategoryEntity::class],
    version = 1,
    exportSchema = true
)
abstract class ComicDatabase : RoomDatabase() {

    abstract fun comicsDao(): ComicsDao

    abstract fun categoryDao(): CategoryDao

    companion object {

        @Volatile
        private var INSTANCE: ComicDatabase? = null

        fun getDatabase(context: Context, databaseCallback: DatabaseCallback): ComicDatabase {
            return INSTANCE ?: synchronized(this) {
                val builder = Room.databaseBuilder(
                    context.applicationContext,
                    ComicDatabase::class.java,
                    "comiqueta_database"
                ).addCallback(databaseCallback)
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