package com.entertainmentbrowser.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import java.io.File
import java.io.FileOutputStream

/**
 * Utility object for bitmap operations including sampling and compression.
 * Optimizes memory usage by decoding images at appropriate sample sizes
 * and compressing them using WebP format.
 */
object BitmapManager {
    
    /**
     * Decodes a bitmap from a file with appropriate sampling to reduce memory usage.
     * The bitmap is scaled down to fit within the requested dimensions while maintaining aspect ratio.
     *
     * @param file The file to decode
     * @param reqWidth The requested width in pixels
     * @param reqHeight The requested height in pixels
     * @return The decoded bitmap, or null if decoding fails
     */
    fun decodeSampledBitmap(file: File, reqWidth: Int, reqHeight: Int): Bitmap? {
        return try {
            // First decode with inJustDecodeBounds=true to check dimensions
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(file.absolutePath, options)
            
            // Calculate inSampleSize
            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
            
            // Decode bitmap with inSampleSize set
            options.inJustDecodeBounds = false
            BitmapFactory.decodeFile(file.absolutePath, options)
        } catch (e: Exception) {
            Log.e("BitmapManager", "Error decoding sampled bitmap from file: ${file.name}", e)
            null
        }
    }
    
    /**
     * Calculates the appropriate sample size for bitmap decoding.
     * The sample size is a power of 2 that results in dimensions that are both
     * larger than or equal to the requested dimensions.
     *
     * @param options BitmapFactory.Options containing the original image dimensions
     * @param reqWidth The requested width in pixels
     * @param reqHeight The requested height in pixels
     * @return The calculated sample size (power of 2)
     */
    fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        // Raw height and width of image
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1
        
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            
            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        
        return inSampleSize
    }
    
    /**
     * Compresses a bitmap to a file using WebP format at 60% quality.
     * WebP provides better compression than PNG while maintaining good quality.
     * Reduced quality to save GPU memory on devices with Mali GPUs.
     *
     * @param bitmap The bitmap to compress
     * @param outputFile The file to write the compressed bitmap to
     * @return True if compression was successful, false otherwise
     */
    fun compressBitmap(bitmap: Bitmap, outputFile: File): Boolean {
        return try {
            FileOutputStream(outputFile).use { outputStream ->
                // Compress using WebP format at 60% quality (reduced from 80% to save GPU memory)
                val compressed = bitmap.compress(Bitmap.CompressFormat.WEBP, 60, outputStream)
                
                if (compressed) {
                    Log.d("BitmapManager", "Bitmap compressed successfully to: ${outputFile.name}")
                } else {
                    Log.w("BitmapManager", "Failed to compress bitmap to: ${outputFile.name}")
                }
                
                compressed
            }
        } catch (e: Exception) {
            Log.e("BitmapManager", "Error compressing bitmap to file: ${outputFile.name}", e)
            false
        }
    }
}
