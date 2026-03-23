package com.example.screenshotminer

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onOpenSettings: () -> Unit,
    onStartService: () -> Unit,
    onStopService: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ScreenshotMiner") },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp)
        ) {

            Button(
                onClick = onStartService,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("START SERVICE")
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onStopService,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("STOP SERVICE")
            }
        }
    }
}