package dev.diegoflassa.comiqueta.core.data.database.converters // Or your chosen package

import android.net.Uri
import androidx.room.TypeConverter
import androidx.core.net.toUri

object UriConverter {
    @TypeConverter
    @JvmStatic
    fun fromString(value: String?): Uri? {
        return value?.toUri() ?: Uri.EMPTY
    }

    @TypeConverter
    @JvmStatic
    fun toString(uri: Uri?): String? {
        return uri?.toString() ?: ""
    }
}
    