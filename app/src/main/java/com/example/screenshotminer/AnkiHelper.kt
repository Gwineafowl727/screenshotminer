package com.example.screenshotminer

import android.content.Context
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

/**
 * Class that contains helper functions for all of the Ankiconnect Android/Ankidroid stuff.
 */
class AnkiHelper() {

    private val apiUrl = "http://127.0.0.1:8765"

    /**
     * The Full Process:
     * Finds newest note, moves media, and updates the field.
     */
    fun processScreenshot(
        context: Context,
        imagePath: String,
        isDeleteEnabled: Boolean,
        targetFieldName: String) {
        thread {
            try {
                val sourceFile = File(imagePath)
                if (!sourceFile.exists()) {
                    println("LOG: Error - Screenshot file not found at $imagePath")
                    return@thread
                }

                // just a starting name; if anki changes it, we account for it in a later step
                val initialName = sourceFile.name.replace(" ", "_")

                // get the most recent note id from within the last 24 hours
                val latestId = getLatestNoteId() ?: run {
                    println("LOG: FAILED - No notes found for last 24 hours.")
                    return@thread
                }

                // a bunch of stuff that encodes the image to be sent to Ankiconnect Android
                println("LOG: Encoding image...")
                val bytes = sourceFile.readBytes()
                val base64Data =
                    android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)

                val mediaJson = JSONObject().apply {
                    put("action", "storeMediaFile")
                    put("version", 6)
                    put("params", JSONObject().apply {
                        put("filename", initialName)
                        put("data", base64Data)
                    })
                }

                val mediaResponse = sendApiRequest(mediaJson)

                // getting the final file name that Ankidroid uses
                // to make sure we insert the right string into card
                val finalNameOnServer = mediaResponse?.optString("result")

                if (finalNameOnServer != null && finalNameOnServer != "null") {
                    println("LOG: File saved to Ankidroid as: $finalNameOnServer")

                    if (isDeleteEnabled) {deleteScreenshot(context, imagePath)}

                    // put the correct file name on the card
                    val imgTag = "<img src=\"$finalNameOnServer\">"
                    val updateSuccess = updateNoteField(latestId, targetFieldName, imgTag)

                    if (updateSuccess) {
                        println("LOG: SUCCESS! Image is live on card: $imgTag")
                    } else {
                        println("LOG: FAILED - Could not update card field.")
                    }
                } else {
                    val error = mediaResponse?.optString("error")
                    println("LOG: FAILED - Anki rejected media: $error")
                }

            } catch (e: Exception) {
                println("LOG: CRITICAL ERROR: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    /**
     * Returns the note id of the card we are going to insert into.
     * This is for AFTER screenshot is taken and is not for detecting a new card.
     */
    private fun getLatestNoteId(): Long? {
        val json = JSONObject().apply {
            put("action", "findNotes")
            put("version", 6)
            put("params", JSONObject().apply { put("query", "added:1") })
        }
        val response = sendApiRequest(json)
        val ids = response?.optJSONArray("result")

        if (ids == null || ids.length() == 0) return null

        var maxId = 0L
        for (i in 0 until ids.length()) {
            val id = ids.getLong(i)
            if (id > maxId) maxId = id
        }
        return if (maxId == 0L) null else maxId
    }

    /**
     * Inserts the string value of the picture into the right field.
     */
    private fun updateNoteField(noteId: Long, fieldName: String, content: String): Boolean {
        val json = JSONObject().apply {
            put("action", "updateNoteFields")
            put("version", 6)
            put("params", JSONObject().apply {
                put("note", JSONObject().apply {
                    put("id", noteId)
                    put("fields", JSONObject().apply { put(fieldName, content) })
                })
            })
        }
        val response = sendApiRequest(json)
        return response?.isNull("error") ?: false
    }

    /**
     * Takes in JSONObject of instructions for what to do with Ankiconnect
     */
    private fun sendApiRequest(json: JSONObject): JSONObject? {
        return try {
            val url = URL(apiUrl)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.connectTimeout = 3000
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")
            conn.outputStream.use { it.write(json.toString().toByteArray()) }

            if (conn.responseCode == 200) {
                val text = conn.inputStream.bufferedReader().use { it.readText() }
                JSONObject(text)
            } else null
        } catch (e: Exception) {
            println("LOG: API ERROR: ${e.message}")
            null
        }
    }

    /**
     * this retrieves a list of all note IDs added in the last 24 hours (we only use the highest though).
     * Anki note IDs are generated in ascending order, so the most recent note ID is numerically higher
     * than all other IDs in the collection.
     */
    fun getMostRecentNoteId(): Long {
        val json = JSONObject().apply {
            put("action", "findNotes")
            put("version", 6)
            put("params", JSONObject().put("query", "added:1"))
        }

        val response = sendApiRequest(json) ?: run {
            println("LOG: Due to API error, returning -1L as most recent id")
            return -1L
        }
        val resultArr = response.optJSONArray("result") ?: run {
            println("LOG: Due to JSON error in extracting card array, returning -1L as most recent id")
            return -1L
        }

        var maxId = -1L
        for (i in 0 until resultArr.length()) {
            val currentId = resultArr.optLong(i, -1L)
            if (currentId > maxId) maxId = currentId
        }
        return maxId
    }

    /**
     * If user has auto-delete enabled, this gets called after image is put into Anki collection folder.
     */
    private fun deleteScreenshot(context: Context, imagePath: String) {
        try {
            val file = File(imagePath)
            if (file.exists()) {
                // 1. Delete the physical file directly from storage
                val deleted = file.delete()

                if (deleted) {
                    println("LOG: Physical file deleted successfully.")

                    // tell the Android Gallery to remove the entry from its database
                    // this prevents "ghost" or "broken" images from appearing in the Gallery app
                    val mediaUri = android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                    val where = "${android.provider.MediaStore.Images.Media.DATA} = ?"
                    val selectionArgs = arrayOf(imagePath)

                    context.contentResolver.delete(mediaUri, where, selectionArgs)
                } else {
                    println("LOG: File.delete() failed. This usually means a permission issue.")
                }
            }
        } catch (e: Exception) {
            println("LOG: Critical delete error: ${e.message}")
        }
    }

}