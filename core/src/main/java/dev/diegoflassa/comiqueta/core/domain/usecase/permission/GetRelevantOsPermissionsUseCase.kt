package dev.diegoflassa.comiqueta.core.domain.usecase.permission

import android.Manifest
import android.os.Build
import javax.inject.Inject

/**
 * Use case to determine the list of relevant OS permissions based on the Android SDK version.
 */
open class GetRelevantOsPermissionsUseCase @Inject constructor() {
    open operator fun invoke(): List<String> {
        return if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
            listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        } else {
            emptyList()
        }
    }
}
