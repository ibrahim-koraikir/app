package com.entertainmentbrowser.core.error

sealed class AppError(open val message: String, open val exception: Throwable? = null) {
    
    // Network errors
    data class NetworkError(
        override val message: String = "No internet connection",
        override val exception: Throwable? = null
    ) : AppError(message, exception)
    
    data class ServerError(
        override val message: String = "Server error occurred",
        override val exception: Throwable? = null
    ) : AppError(message, exception)
    
    data class TimeoutError(
        override val message: String = "Request timed out",
        override val exception: Throwable? = null
    ) : AppError(message, exception)
    
    // Database errors
    data class DatabaseError(
        override val message: String = "Database operation failed",
        override val exception: Throwable? = null
    ) : AppError(message, exception)
    
    // Storage errors
    data class StorageError(
        override val message: String = "Storage operation failed",
        override val exception: Throwable? = null
    ) : AppError(message, exception)
    
    data class PermissionError(
        override val message: String = "Permission denied",
        override val exception: Throwable? = null
    ) : AppError(message, exception)
    
    // Download errors
    data class DownloadError(
        override val message: String = "Download failed",
        override val exception: Throwable? = null
    ) : AppError(message, exception)
    
    data class DrmError(
        override val message: String = "Content is DRM protected and cannot be downloaded",
        override val exception: Throwable? = null
    ) : AppError(message, exception)
    
    data class UnsupportedFormatError(
        override val message: String = "Video format not supported for download",
        override val exception: Throwable? = null
    ) : AppError(message, exception)
    
    // WebView errors
    data class WebViewError(
        override val message: String = "Failed to load website",
        override val exception: Throwable? = null
    ) : AppError(message, exception)
    
    // Generic errors
    data class UnknownError(
        override val message: String = "An unexpected error occurred",
        override val exception: Throwable? = null
    ) : AppError(message, exception)
    
    data class ValidationError(
        override val message: String = "Validation failed",
        override val exception: Throwable? = null
    ) : AppError(message, exception)
}

fun Throwable.toAppError(): AppError {
    return when (this) {
        is java.net.UnknownHostException -> AppError.NetworkError(exception = this)
        is java.net.SocketTimeoutException -> AppError.TimeoutError(exception = this)
        is java.io.IOException -> AppError.NetworkError(exception = this)
        else -> AppError.UnknownError(message = this.message ?: "Unknown error", exception = this)
    }
}
