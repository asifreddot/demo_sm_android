package com.reddot.mvvmtodo.ui.baseClass

import android.util.Log
import retrofit2.Response
import com.reddot.mvvmtodo.ui.Result

/**
 * Abstract Base Data source class with error handling
 */
abstract class BaseDataSource() {
    private val TAG = "BaseDataSource"
    protected suspend fun <T> getResult(call: suspend () -> Response<T>): Result<T> {
        try {

            val response = call()
            Log.d(TAG, "getResult: $response")
            if (response.isSuccessful) {
                val body = response.body()
                return if (body != null)
                    Result.success(body)
                else
                    Result.success()
            } else if (response.code() == 401) {
                return Result.error(
                        message = "UNAUTHORIZED",
                        errorCode = response.code()
                )
            } else {
                return Result.error(
                        message = "API_ERROR",
                        errorCode = response.code()
                )
            }
            // return processError(response)
        } catch (e: Exception) {
            e.printStackTrace()
            Log.d(TAG, "getResult: ${e.message}")

            return Result.error(
                    message = "NETWORK_CONNECT_TIMEOUT_ERROR",
                    errorCode = 599
            )
        }
    }

}