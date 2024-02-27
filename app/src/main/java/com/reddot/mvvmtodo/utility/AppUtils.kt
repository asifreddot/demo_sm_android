package com.reddot.mvvmtodo.utility

class AppUtils {
    companion object {
        fun extractUsernameFromEmail(email: String?): String? {
            return try {
                val regex = "^(.+)@.*$".toRegex()
                val matchResult = email?.let { regex.find(it) }
                matchResult?.groups?.get(1)?.value
            } catch (e:java.lang.Exception) {
                ""
            }

        }
    }
}