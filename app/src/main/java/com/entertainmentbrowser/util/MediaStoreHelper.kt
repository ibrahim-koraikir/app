package com.entertainmentbrowser.util

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.FileInputStream
import java.io.OutputStream

object MediaStoreHelper {
    
    /**
     * Save a downloaded file to MediaStore for Android 10+ (scoped storage)
     * or to Downloads directory for older versions
     */
    fun saveToMediaStore(
        context: Context,
        sourceFile: File,
        filename: String,
        mimeType: String = "video/*"
    ): Uri? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Use MediaStore for Android 10+
            saveToMediaStoreApi29Plus(context, sourceFile, filename, mimeType)
        } else {
            // Use legacy file system for older versions
            saveToLegacyStorage(sourceFile, filename)
        }
    }
    
    /**
     * Save file using MediaStore API for Android 10+
     */
    @androidx.annotation.RequiresApi(android.os.Build.VERSION_CODES.Q)
    private fun saveToMediaStoreApi29Plus(
        context: Context,
        sourceFile: File,
        filename: String,
        mimeType: String
    ): Uri? {
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }
        
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
        
        uri?.let {
            try {
                resolver.openOutputStream(it)?.use { outputStream ->
                    copyFile(sourceFile, outputStream)
                }
                
                // Mark as not pending anymore
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    resolver.update(it, contentValues, null, null)
                }
                
                return it
            } catch (e: Exception) {
                // Clean up on failure
                resolver.delete(it, null, null)
                throw e
            }
        }
        
        return null
    }
    
    /**
     * Save file to Downloads directory for Android 9 and below
     */
    private fun saveToLegacyStorage(sourceFile: File, filename: String): Uri? {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (!downloadsDir.exists()) {
            downloadsDir.mkdirs()
        }
        
        val destFile = File(downloadsDir, filename)
        sourceFile.copyTo(destFile, overwrite = true)
        
        return Uri.fromFile(destFile)
    }
    
    /**
     * Copy file content from source to output stream
     */
    private fun copyFile(sourceFile: File, outputStream: OutputStream) {
        FileInputStream(sourceFile).use { inputStream ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
            }
        }
    }
    
    /**
     * Generate a content URI for a file
     */
    fun getContentUri(context: Context, filename: String): Uri? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            getContentUriApi29Plus(context, filename)
        } else {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val file = File(downloadsDir, filename)
            if (file.exists()) {
                Uri.fromFile(file)
            } else {
                null
            }
        }
    }
    
    @androidx.annotation.RequiresApi(android.os.Build.VERSION_CODES.Q)
    private fun getContentUriApi29Plus(context: Context, filename: String): Uri? {
        val projection = arrayOf(MediaStore.MediaColumns._ID)
        val selection = "${MediaStore.MediaColumns.DISPLAY_NAME} = ?"
        val selectionArgs = arrayOf(filename)
        
        context.contentResolver.query(
            MediaStore.Downloads.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID))
                return Uri.withAppendedPath(MediaStore.Downloads.EXTERNAL_CONTENT_URI, id.toString())
            }
        }
        return null
    }
    
    /**
     * Get MIME type from filename
     */
    fun getMimeType(filename: String): String {
        return when (filename.substringAfterLast('.', "").lowercase()) {
            "mp4" -> "video/mp4"
            "webm" -> "video/webm"
            "m3u8" -> "application/vnd.apple.mpegurl"
            "mpd" -> "application/dash+xml"
            else -> "video/*"
        }
    }
}
