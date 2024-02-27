package com.reddot.mvvmtodo.ui.baseClass

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.google.firebase.auth.FirebaseAuth

open class BaseActivity : AppCompatActivity() {
    protected lateinit var mFirebaseAuth: FirebaseAuth
    override fun onCreate(savedInstanceState: Bundle?) {
        mFirebaseAuth = FirebaseAuth.getInstance()
        super.onCreate(savedInstanceState)

    }

    fun <T> redirectToActivity(redirectActivity: Class<T>) {
        startActivity(Intent(this@BaseActivity, redirectActivity))
        finish()
    }

    fun <T> redirectToActivityWithoutFinish(redirectActivity: Class<T>) {
        startActivity(Intent(this@BaseActivity, redirectActivity))
    }
}