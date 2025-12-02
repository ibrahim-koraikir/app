package com.entertainmentbrowser.util

import android.net.Uri
import android.util.Log
import com.entertainmentbrowser.BuildConfig

/**
 * Logging utilities with privacy protection.
 * 
 * **Privacy Policy:**
 * - In DEBUG builds: Full URLs are logged for development/debugging
 * - In RELEASE builds: URLs are redacted to domain only, protecting user privacy
 * 
 * This prevents sensitive browsing data (query parameters, paths, etc.) from
 * appearing in production logs or crash reports.
 */
object LogUtils {
    
    /**
     * Redacts sensitive parts of a URL for safe logging.
     * 
     * **DEBUG builds:** Returns full URL for debugging
     * **RELEASE builds:** Returns only scheme + domain (e.g., "https://example.com")
     * 
     * Examples:
     * - `https://example.com/path?query=secret` → `https://example.com` (release)
     * - `https://example.com/path?query=secret` → `https://example.com/path?query=secret` (debug)
     * 
     * @param url The URL to redact
     * @return Redacted URL safe for logging
     */
    fun redactUrl(url: String?): String {
        if (url.isNullOrBlank()) return "[empty]"
        
        // In debug builds, log full URL for development
        if (BuildConfig.DEBUG) {
            return url
        }
        
        // In release builds, redact to domain only
        return try {
            val uri = Uri.parse(url)
            val scheme = uri.scheme ?: "unknown"
            val host = uri.host ?: "[no-host]"
            "$scheme://$host"
        } catch (e: Exception) {
            "[invalid-url]"
        }
    }
    
    /**
     * Log debug message with URL redaction.
     * 
     * @param tag Log tag
     * @param message Message template (use %s for URL placeholder)
     * @param url URL to redact and log
     */
    fun d(tag: String, message: String, url: String? = null) {
        if (BuildConfig.DEBUG) {
            if (url != null) {
                Log.d(tag, message.format(redactUrl(url)))
            } else {
                Log.d(tag, message)
            }
        }
    }
    
    /**
     * Log warning message with URL redaction.
     * 
     * @param tag Log tag
     * @param message Message template (use %s for URL placeholder)
     * @param url URL to redact and log
     */
    fun w(tag: String, message: String, url: String? = null) {
        if (url != null) {
            Log.w(tag, message.format(redactUrl(url)))
        } else {
            Log.w(tag, message)
        }
    }
    
    /**
     * Log error message with URL redaction.
     * 
     * @param tag Log tag
     * @param message Message template (use %s for URL placeholder)
     * @param url URL to redact and log
     * @param throwable Optional exception
     */
    fun e(tag: String, message: String, url: String? = null, throwable: Throwable? = null) {
        val formattedMessage = if (url != null) message.format(redactUrl(url)) else message
        if (throwable != null) {
            Log.e(tag, formattedMessage, throwable)
        } else {
            Log.e(tag, formattedMessage)
        }
    }
}
