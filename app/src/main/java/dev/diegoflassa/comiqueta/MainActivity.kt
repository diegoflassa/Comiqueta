package dev.diegoflassa.comiqueta

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.google.android.gms.ads.MobileAds
import dagger.hilt.android.AndroidEntryPoint
import dev.diegoflassa.comiqueta.core.data.timber.TimberLogger
import dev.diegoflassa.comiqueta.core.data.worker.SafFolderScanWorker
import dev.diegoflassa.comiqueta.core.navigation.NavigationViewModel
import dev.diegoflassa.comiqueta.core.theme.ComiquetaTheme
import dev.diegoflassa.comiqueta.core.ui.hiltActivityViewModel
import dev.diegoflassa.comiqueta.navigation.NavDisplay

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private lateinit var openDirectoryLauncher: ActivityResultLauncher<Uri?>
    private lateinit var runtimePermissionLauncher: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MobileAds.initialize(this) {}

        setupLaunchers()

        setContent {
            val navigationViewModel: NavigationViewModel = hiltActivityViewModel()
            ComiquetaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NavDisplay(modifier = Modifier, navigationViewModel = navigationViewModel)
                }
            }

            BackHandler {
                navigationViewModel.goBack()
            }
        }
    }

    private fun setupLaunchers() {
        runtimePermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
                if (isGranted) {
                    TimberLogger.logD("MainActivity", "Runtime permission granted.")
                    openDirectoryLauncher.launch(null) // Proceed to open directory
                } else {
                    TimberLogger.logW("MainActivity", "Runtime permission denied.")
                    Toast.makeText(
                        this,
                        "Storage permission is required to select a folder on this Android version.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

        openDirectoryLauncher =
            registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { treeUri ->
                if (treeUri != null) {
                    val contentResolver = applicationContext.contentResolver
                    try {
                        contentResolver.takePersistableUriPermission(
                            treeUri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                        )
                        TimberLogger.logD(
                            "MainActivity",
                            "Persistable URI permission taken for: $treeUri"
                        )
                        startFolderScanWorker(treeUri)
                    } catch (e: SecurityException) {
                        TimberLogger.logE(
                            "MainActivity",
                            "Failed to take persistable URI permission",
                            e
                        )
                        Toast.makeText(
                            this,
                            "Failed to get long-term access to the folder.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } else {
                    TimberLogger.logD("MainActivity", "No folder selected.")
                }
            }
    }

    /**
     * Call this method when the user wants to select a folder.
     * On API 32 and below, it handles asking for READ_EXTERNAL_STORAGE.
     * On API 33 and above, it directly launches the SAF folder picker based on current constraints.
     */
    fun requestFolderAccess() {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) { // API 32 or older
            val permissionToRequest = Manifest.permission.READ_EXTERNAL_STORAGE
            TimberLogger.logD(
                "MainActivity",
                "Requesting folder access on API ${Build.VERSION.SDK_INT}, need permission: $permissionToRequest"
            )
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    permissionToRequest
                ) == PackageManager.PERMISSION_GRANTED -> {
                    TimberLogger.logD(
                        "MainActivity",
                        "$permissionToRequest already granted. Launching directory picker."
                    )
                    openDirectoryLauncher.launch(null)
                }
                else -> {
                    TimberLogger.logD(
                        "MainActivity",
                        "Requesting runtime permission: $permissionToRequest"
                    )
                    runtimePermissionLauncher.launch(permissionToRequest)
                }
            }
        } else { // API 33 or newer
            TimberLogger.logD(
                "MainActivity",
                "Requesting folder access on API ${Build.VERSION.SDK_INT}. No runtime permission from the specified list will be requested. Launching directory picker directly."
            )
            // Based on the current constraints (not asking for READ_MEDIA_IMAGES here),
            // we directly launch the SAF picker. The picker itself handles access.
            openDirectoryLauncher.launch(null)
        }
    }

    private fun startFolderScanWorker(folderUri: Uri) {
        val workInputData = Data.Builder()
            .putString("folderUri", folderUri.toString())
            .build()

        val scanWorkRequest = OneTimeWorkRequestBuilder<SafFolderScanWorker>()
            .setInputData(workInputData)
            .build()

        val workManager = WorkManager.getInstance(applicationContext)
        workManager.enqueueUniqueWork(
            "folderScanWorker", // Unique work name
            ExistingWorkPolicy.REPLACE, // Replace existing work if any
            scanWorkRequest
        )
        TimberLogger.logD("MainActivity", "SafFolderScanWorker enqueued for URI: $folderUri")

        workManager.getWorkInfoByIdLiveData(scanWorkRequest.id)
            .observe(this) { workInfo ->
                if (workInfo != null) {
                    TimberLogger.logD("MainActivity", "Worker status: ${workInfo.state}")
                    if (workInfo.state == androidx.work.WorkInfo.State.FAILED) {
                        TimberLogger.logW("MainActivity", "Worker failed. Output data: ${workInfo.outputData}")
                    } else if (workInfo.state == androidx.work.WorkInfo.State.SUCCEEDED) {
                        TimberLogger.logD("MainActivity", "Worker succeeded. Output data: ${workInfo.outputData}")
                    }
                }
            }
    }
}
