package dev.diegoflassa.comiqueta.core.data.database.converter

import android.net.Uri
import androidx.core.net.toUri
import androidx.room.TypeConverter

class UriConverters {
    @TypeConverter
    fun fromString(value: String?): Uri? {
        return value?.toUri()
    }

    @TypeConverter
    fun uriToString(uri: Uri?): String? {
        return uri?.toString()
    }
}
