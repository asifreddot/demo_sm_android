package com.reddot.mvvmtodo.ui

import android.os.Bundle
import android.os.Handler
import androidx.lifecycle.lifecycleScope
import com.reddot.mvvmtodo.R
import com.reddot.mvvmtodo.data.PreferencesManager
import com.reddot.mvvmtodo.ui.baseClass.BaseActivity
import com.reddot.mvvmtodo.ui.login.UserLoginActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SplashScreen : BaseActivity() {

    @Inject
    lateinit var preferencesManager: PreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)


        Handler().postDelayed({

            lifecycleScope.launch(Dispatchers.Main) {
                if (preferencesManager.getAccessToken().isNullOrEmpty()) {
                    redirectToActivity(UserLoginActivity::class.java)
                } else {
                    redirectToActivity(MainActivity::class.java)
                }
            }

        }, 2000)
    }
}