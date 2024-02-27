package com.reddot.mvvmtodo.ui.login

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reddot.mvvmtodo.data.PreferencesManager
import com.reddot.mvvmtodo.repository.DataSourceRepository
import kotlinx.coroutines.launch

class UserLoginViewModel @ViewModelInject constructor(
    private val repository: DataSourceRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    fun fetchLoginResponse(email: String, password: String) =
        repository.verifyUserLogin(email, password)

    fun fetchSocialLoginResponse(providerId: String, name: String, email: String) =
        repository.verifyUserSocialLogin(providerId, name, email)

    fun updateAccessToken(accessToken: String) = viewModelScope.launch {
        preferencesManager.updateAccessToken(accessToken)
    }
}