package dev.diegoflassa.comiqueta

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.google.android.gms.ads.MobileAds
import dev.diegoflassa.comiqueta.core.data.worker.SafFolderScanWorker
import dev.diegoflassa.comiqueta.core_ui.theme.ComiquetaTheme
import dagger.hilt.android.AndroidEntryPoint
import dev.diegoflassa.comiqueta.core.data.timber.TimberLogger
import javax.inject.Inject

@AndroidEntryPoint // Important for Hilt
class MainActivity : ComponentActivity() {
    private lateinit var openDirectoryLauncher: ActivityResultLauncher<Uri?>
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MobileAds.initialize(this) {}

        requestPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
                if (isGranted) {
                    TimberLogger.logD("MainActivity", "READ_EXTERNAL_STORAGE permission granted.")
                    // Permission is granted. You can now launch the directory picker.
                    openDirectoryLauncher.launch(null)
                } else {
                    TimberLogger.logW("MainActivity", "READ_EXTERNAL_STORAGE permission denied.")
                    Toast.makeText(this, "Storage permission is required to select a folder.", Toast.LENGTH_LONG).show()
                    // Explain to the user that the feature is unavailable because the
                    // features requires a permission that the user has denied.
                    // At the same time, respect the user's decision. Don't link to
                    // system settings in an effort to convince the user to change their
                    // decision.
                }
            }

        openDirectoryLauncher =
            registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { treeUri ->
                if (treeUri != null) {
                    // Persist URI permission for long-term access
                    val contentResolver = applicationContext.contentResolver
                    try {
                        contentResolver.takePersistableUriPermission(
                            treeUri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                        )
                        TimberLogger.logD("MainActivity", "Persistable URI permission taken for: $treeUri")
                        startFolderScanWorker(treeUri)
                    } catch (e: SecurityException) {
                        TimberLogger.logE("MainActivity", "Failed to take persistable URI permission", e)
                        Toast.makeText(this, "Failed to get long-term access to the folder.", Toast.LENGTH_LONG).show()
                    }
                } else {
                    TimberLogger.logD("MainActivity", "No folder selected.")
                }
            }

        setContent {
            ComiquetaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(
                        onSelectFolder = {
                            when {
                                ContextCompat.checkSelfPermission(
                                    this,
                                    Manifest.permission.READ_EXTERNAL_STORAGE
                                ) == PackageManager.PERMISSION_GRANTED -> {
                                    // Permission is already granted, launch the directory picker.
                                    openDirectoryLauncher.launch(null)
                                }
                                shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE) -> {
                                    // Explain to the user why you need this permission.
                                    // For now, just requesting directly. You might want to show a dialog first.
                                    TimberLogger.logI("MainActivity", "Showing permission rationale for READ_EXTERNAL_STORAGE.")
                                    Toast.makeText(this, "Storage permission is needed to browse folders.", Toast.LENGTH_LONG).show()
                                    requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                                }
                                else -> {
                                    // Directly request the permission.
                                    requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                                }
                            }
                        }
                    )
                }
            }
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
            "folderScanWorker",
            ExistingWorkPolicy.REPLACE,
            scanWorkRequest
        )

        TimberLogger.logD("MainActivity", "SafFolderScanWorker enqueued for URI: $folderUri")

        workManager.getWorkInfoByIdLiveData(scanWorkRequest.id)
            .observe(this) { workInfo ->
                if (workInfo != null) {
                    TimberLogger.logD("MainActivity", "Worker State: ${workInfo.state}")
                    when (workInfo.state) {
                        WorkInfo.State.SUCCEEDED -> TimberLogger.logI("MainActivity", "Folder scan succeeded.")
                        WorkInfo.State.FAILED -> TimberLogger.logE("MainActivity", "Folder scan failed.")
                        WorkInfo.State.ENQUEUED -> TimberLogger.logD("MainActivity", "Folder scan enqueued.")
                        WorkInfo.State.RUNNING -> TimberLogger.logD("MainActivity", "Folder scan running.")
                        WorkInfo.State.BLOCKED -> TimberLogger.logD("MainActivity", "Folder scan blocked.")
                        WorkInfo.State.CANCELLED -> TimberLogger.logD("MainActivity", "Folder scan cancelled.")
                    }
                }
            }
    }
}

@Composable
fun MainScreen(onSelectFolder: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Greeting("Android")
        Button(onClick = onSelectFolder) {
            Text("Select Folder to Scan")
        }
    }
}

@Composable
fun Greeting(name: String) {
    Text(text = "Hello $name!")
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    ComiquetaTheme {
        MainScreen(onSelectFolder = {})
    }
}