package com.example.screenshotminer

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import com.example.screenshotminer.ui.theme.ScreenshotMinerTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModelProvider

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val viewModel = ViewModelProvider(this)[MainViewModel::class.java]

        setContent {

            val context = LocalContext.current
            var showSettings by remember { mutableStateOf(false) }

            // permission stuff
            val permissionsLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestMultiplePermissions()
            ) { results ->
                // results is a Map<String, Boolean>
                val storageOk = results[android.Manifest.permission.READ_MEDIA_IMAGES] == true ||
                        results[android.Manifest.permission.READ_EXTERNAL_STORAGE] == true
                val notifOk = results[android.Manifest.permission.POST_NOTIFICATIONS]
                val ankiOk = results["com.ichi2.anki.permission.READ_WRITE_DATABASE"] == true

                println("LOG: Storage=$storageOk, Notif=$notifOk, Anki=$ankiOk")
            }

            LaunchedEffect(Unit) {
                val list = mutableListOf<String>()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    list.add(android.Manifest.permission.READ_MEDIA_IMAGES)
                    // for tiramisu+
                    list.add(android.Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    list.add(android.Manifest.permission.READ_EXTERNAL_STORAGE)
                }

                list.add("com.ichi2.anki.permission.READ_WRITE_DATABASE")
                permissionsLauncher.launch(list.toTypedArray())
            }

            ScreenshotMinerTheme {
                if (showSettings) {
                    SettingsScreen(
                        viewModel = viewModel,
                        onNavigateBack = { showSettings = false }
                    )
                } else {
                    DashboardScreen(
                        onOpenSettings = { showSettings = true },
                        onStartService = {
                            val intent = Intent(context, ScreenshotMinerService::class.java)
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                context.startForegroundService(intent)
                            } else {
                                context.startService(intent)
                            }
                        },
                        onStopService = {
                            val intent = Intent(context, ScreenshotMinerService::class.java)
                            context.stopService(intent)
                        }
                    )
                }
            }
        }
    }
}


