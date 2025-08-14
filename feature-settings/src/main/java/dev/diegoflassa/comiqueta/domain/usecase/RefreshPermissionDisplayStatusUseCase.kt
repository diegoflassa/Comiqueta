package dev.diegoflassa.comiqueta.domain.usecase

import android.app.Activity
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import dev.diegoflassa.comiqueta.core.domain.usecase.permission.GetRelevantOsPermissionsUseCase
import dev.diegoflassa.comiqueta.data.model.PermissionDisplayStatus
import javax.inject.Inject

class RefreshPermissionDisplayStatusUseCase @Inject constructor(
    private val getRelevantOsPermissionsUseCase: GetRelevantOsPermissionsUseCase
) : IRefreshPermissionDisplayStatusUseCase {
    override operator fun invoke(activity: Activity): Map<String, PermissionDisplayStatus> {
        val relevantPermissions = getRelevantOsPermissionsUseCase()
        if (relevantPermissions.isEmpty()) {
            return emptyMap()
        }
        return relevantPermissions.associateWith { permission ->
            val isGranted = ContextCompat.checkSelfPermission(
                activity,
                permission
            ) == PackageManager.PERMISSION_GRANTED
            val shouldShowRationale =
                !isGranted && ActivityCompat.shouldShowRequestPermissionRationale(
                    activity,
                    permission
                )
            PermissionDisplayStatus(isGranted, shouldShowRationale)
        }
    }
}
