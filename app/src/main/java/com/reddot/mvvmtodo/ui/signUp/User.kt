package com.reddot.mvvmtodo.ui.signUp

import com.google.gson.annotations.SerializedName

data class User(
    var userId: String? = null,
    @SerializedName("name") var name: String? = null,
    @SerializedName("email") var email: String? = null,
    @SerializedName("phone") var phone: String? = null,
    @SerializedName("password") var password: String? = null,
    @SerializedName("role") var role: String? = null
)
