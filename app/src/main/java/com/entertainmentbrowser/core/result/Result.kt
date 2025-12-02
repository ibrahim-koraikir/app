package com.entertainmentbrowser.core.result

sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val message: String, val exception: Throwable? = null) : Result<Nothing>()
    object Loading : Result<Nothing>()
}

fun <T> Result<T>.onSuccess(action: (T) -> Unit): Result<T> {
    if (this is Result.Success) action(data)
    return this
}

fun <T> Result<T>.onError(action: (String) -> Unit): Result<T> {
    if (this is Result.Error) action(message)
    return this
}

fun <T> Result<T>.getOrNull(): T? {
    return if (this is Result.Success) data else null
}

fun <T> Result<T>.isSuccess(): Boolean {
    return this is Result.Success
}

fun <T> Result<T>.isError(): Boolean {
    return this is Result.Error
}

fun <T> Result<T>.isLoading(): Boolean {
    return this is Result.Loading
}
