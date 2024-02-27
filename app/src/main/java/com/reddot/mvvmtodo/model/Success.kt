package com.reddot.mvvmtodo.model

import com.google.gson.annotations.SerializedName

data class Success(
    val `data`: Data,
    val message: String,
    val success: Boolean,
    @SerializedName("status")
    val status_code: Int
)