package com.reddot.mvvmtodo.ui

data class Result<out T>(
        val status: Status,
        val data: T?,
        var message: String?,
        val errorCode: Int? = null
) {

    enum class Status {
        SUCCESS,
        ERROR,
        LOADING
    }

    companion object {

        fun <T> success(data: T, message: String? = null): Result<T> {
            return Result(Status.SUCCESS, data, message)
        }

        fun <T> success(message: String? = null): Result<T> {
            return Result(Status.SUCCESS, null, message)
        }

        fun <T> error(message: String, data: T? = null): Result<T> {
            return Result(Status.ERROR, data, message)
        }

        fun <T> error(message: String? = null, data: T? = null, errorCode: Int? = null): Result<T> {
            return Result(Status.ERROR, data, message, errorCode)
        }

        fun <T> loading(data: T? = null): Result<T> {
            return Result(Status.LOADING, data, null)
        }
    }
}