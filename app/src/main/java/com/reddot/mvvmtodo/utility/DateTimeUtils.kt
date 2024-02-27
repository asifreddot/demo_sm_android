package com.reddot.mvvmtodo.utility

import java.text.SimpleDateFormat
import java.util.*

private const val serverDateTimeFormat = "yyyy-MM-dd HH:mm:ss"
class DateTimeUtils {
    companion object{
        fun formatServerDateTime(currentTimeMillis: Long): String {
            val sdf = SimpleDateFormat(serverDateTimeFormat)
            val currentDate = Date(currentTimeMillis)
            return sdf.format(currentDate)
        }
    }
}