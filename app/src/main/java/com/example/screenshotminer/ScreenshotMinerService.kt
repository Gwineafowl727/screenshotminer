package com.example.screenshotminer

import android.app.*
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first

/**
 * Class for the app's background service.
 */
class ScreenshotMinerService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var isRunning = false

    /**
     * The service that watches for card creation and triggers insertionCoroutine appropriately
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (isRunning) return START_STICKY // prevents multiple services from running
        isRunning = true
        println("LOG: onStartCommand reached!")

        try {
            val notification = createNotification()
            startForeground(1, notification)
            println("LOG: startForeground called successfully")
        } catch (e: Exception) {
            println("LOG: FAILED to start foreground: ${e.message}")
            e.printStackTrace()
        }

        serviceScope.launch {
            println("LOG: main service loop started")
            val ankiHelper = AnkiHelper()
            val settingsManager = SettingsManager(this@ScreenshotMinerService)
            var lastMaxId = ankiHelper.getMostRecentNoteId()
            while (isActive) {

                val targetField = settingsManager.targetField.first()
                val miningTimeout = settingsManager.miningTimeout.first()
                val redoTimeout = settingsManager.redoTimeout.first()
                val isDeleteEnabled = settingsManager.isDeleteEnabled.first()

                val currentMaxId = ankiHelper.getMostRecentNoteId()
                println("LOG: currentMaxId: $currentMaxId")

                // triggers when there is a new card
                if (currentMaxId > lastMaxId && currentMaxId != -1L) {
                    println("LOG: New card detected (ID: $currentMaxId)! Waiting for screenshot to insert.")

                    lastMaxId = currentMaxId
                    insertionCoroutine(
                        context = this@ScreenshotMinerService,
                        isDeleteEnabled = isDeleteEnabled,
                        startTime = System.currentTimeMillis(),
                        targetField = targetField,
                        miningTimeout = miningTimeout,
                        redoTimeout = redoTimeout,
                        scope = this, // Uses the service's scope to keep running
                        onResult = { status ->
                            println("LOG: Insertion Status -> $status")
                        }
                    )
                }

                else {
                    lastMaxId = currentMaxId
                }

                delay(500) // check every 0.5 second for a new note
            }
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    /**
     * Creates the persistent notification to keep app alive
     */
    private fun createNotification(): Notification {
        val channelId = "screenshot_miner_channel"
        val channelName = "ScreenshotMiner Background Service"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT)
            manager.createNotificationChannel(channel)
        }

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("ScreenshotMiner is active")
            .setSmallIcon(R.drawable.ic_stat_name)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setOngoing(true)
            .build()
    }

}