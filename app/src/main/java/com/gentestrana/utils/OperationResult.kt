package com.gentestrana.utils

sealed class OperationResult<out T> {
    data class Success<out T>(val data: T): OperationResult<T>()
    data class Error(val message: String): OperationResult<Nothing>()
}
