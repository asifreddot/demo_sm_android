package com.reddot.mvvmtodo.model

data class SignUpResponse(
    val status: Int,
    val data: UserDataDetails,
    val message: String
)

data class UserDataDetails(
    val name: String,
    val email: String,
    val role: String
)