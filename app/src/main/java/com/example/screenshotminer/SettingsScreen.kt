package com.example.screenshotminer

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val savedTargetField by viewModel.targetField.collectAsState()
    val savedMiningTimeout by viewModel.miningTimeout.collectAsState()
    val savedRedoTimeout by viewModel.redoTimeout.collectAsState()
    val savedIsDeleteEnabled by viewModel.isDeleteEnabled.collectAsState()

    var draftTargetField by remember { mutableStateOf(savedTargetField)}
    var draftMiningTimeout by remember { mutableStateOf(savedMiningTimeout.toString())}
    var draftRedoTimeout by remember { mutableStateOf(savedRedoTimeout.toString())}
    var draftIsDeleteEnabled by remember {mutableStateOf(savedIsDeleteEnabled)}

    // makes it so that after user has granted permission to delete pictures,
    // the toggle auto turns on when returning to app settings
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                val hasPerm = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                    android.os.Environment.isExternalStorageManager()
                } else {
                    true
                }

                // If they return and permission is now granted, flip the toggle
                if (hasPerm && !draftIsDeleteEnabled) {
                    draftIsDeleteEnabled = true
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Field Name Input
            OutlinedTextField(
                value = draftTargetField,
                onValueChange = { draftTargetField = it },
                label = { Text("Anki Target Field") },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("e.g., Picture") }
            )

            // Mining Timeout Input
            OutlinedTextField(
                value = draftMiningTimeout,
                onValueChange = { if (it.all { char -> char.isDigit() }) draftMiningTimeout = it },
                label = { Text("Mining Timeout (ms)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                )
            )

            // Redo Timeout Input
            OutlinedTextField(
                value = draftRedoTimeout,
                onValueChange = { if (it.all { char -> char.isDigit() }) draftRedoTimeout = it },
                label = { Text("Redo Timeout (ms)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                )
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Auto-delete Screenshots",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "Delete file after it's added to card",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // if currently disabled, must check permission before enabling.
                // if no permission given, take user to external view to grant permission.
                // when user returns to app settings, enable if permission has been granted.
                val context = androidx.compose.ui.platform.LocalContext.current
                val isAndroid11Plus = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R
                Switch(
                    checked = draftIsDeleteEnabled,
                    onCheckedChange = { newValue ->
                        // user tries to turn it on, but necessary perm is not given
                        if (newValue && isAndroid11Plus && !android.os.Environment.isExternalStorageManager()) {

                            val intent = android.content.Intent(
                                android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION
                            ).apply { data = "package:${context.packageName}".toUri() }
                            context.startActivity(intent)

                        // either user has permissions or it is already on and user is turning it off
                        } else {
                            draftIsDeleteEnabled = newValue
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // save button
            Button(
                onClick = {
                    val miningLong = draftMiningTimeout.toLongOrNull() ?: 60000L
                    val redoLong = draftRedoTimeout.toLongOrNull() ?: 20000L

                    // Update the ViewModel (which saves to DataStore)
                    viewModel.updateSettings(draftTargetField, miningLong, redoLong, draftIsDeleteEnabled)

                    // Return to the main screen
                    onNavigateBack()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("SAVE SETTINGS")
            }
        }
    }

}