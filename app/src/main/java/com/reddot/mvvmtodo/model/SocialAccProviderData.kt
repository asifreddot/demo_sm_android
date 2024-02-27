package com.reddot.mvvmtodo.model

data class SocialAccProviderData(
        var providerId: String,
        var uid: String,
        var displayName: String? = null,
        var email: String? = null,
        var phoneNumber: String? = null,
        var photoURL: String? = null
)
