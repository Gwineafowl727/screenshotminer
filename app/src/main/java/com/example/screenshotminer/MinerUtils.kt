package com.example.screenshotminer

import android.content.Context
import android.provider.MediaStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Gets image path that was created within the alloted time of the function call
 */
fun getValidImage(context: Context, startTime: Long): String? {
    val projection = arrayOf(
        MediaStore.Images.Media.DATA,
        MediaStore.Images.Media.DATE_TAKEN,
        MediaStore.Images.Media.BUCKET_DISPLAY_NAME
    )

    val sortOrder = "${MediaStore.Images.Media.DATE_TAKEN} DESC"
    val cursor = context.contentResolver.query(
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        projection,
        null,
        null,
        sortOrder
    )

    return cursor?.use { c ->
        if (c.moveToFirst()) {
            val dateIndex = c.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
            val path = c.getString(c.getColumnIndexOrThrow(MediaStore.Images.Media.DATA))
            val imageTime = c.getLong(dateIndex)

            if (imageTime > startTime) {
                path
            }
            else {
                null
            }
        } else {
            null
        }
    }

}

/**
 * The entire timing/insertion cycle that gets called right after a card is made.
 */
fun insertionCoroutine(
    context: Context,
    isDeleteEnabled: Boolean,
    startTime: Long,
    targetField: String,
    miningTimeout: Long,
    redoTimeout: Long,
    scope: CoroutineScope,
    onResult: (String) -> Unit
)
{
    scope.launch {

        var currentTimeLimit = startTime + miningTimeout // starts at 1 min
        val checkInterval = 500L
        var lastFoundPath: String? = null

        onResult("Searching...")

        while (System.currentTimeMillis() < currentTimeLimit) {
            val newPath = getValidImage(context, startTime)

            // checks if there is a new file to process
            if (newPath != null && newPath != lastFoundPath) {

                // update path
                lastFoundPath = newPath
                onResult("Found: $newPath")

                // trigger the anki update here
                val ankiHelper = AnkiHelper()
                ankiHelper.processScreenshot(context = context, imagePath = newPath, targetFieldName = targetField, isDeleteEnabled = isDeleteEnabled)

                // change timer to having 20s left after a match is found
                currentTimeLimit = System.currentTimeMillis() + redoTimeout
                println("Screenshot processed, time limit changed to ${redoTimeout / 1000}s left.")
            }
            delay(checkInterval)
        }

        onResult("Screenshot watching has timed out. Back to looking for new card.")
    }
}