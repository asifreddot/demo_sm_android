package com.reddot.mvvmtodo.ui.signUp

import androidx.databinding.ObservableField
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reddot.mvvmtodo.data.PreferencesManager
import com.reddot.mvvmtodo.repository.DataSourceRepository
import kotlinx.coroutines.launch

class SignUpViewModel @ViewModelInject constructor(
    private val repository: DataSourceRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    val email = ObservableField<String>()
    val phone = ObservableField<String>()
    val password = ObservableField<String>()
    val confirmPassword = ObservableField<String>()

    fun fetchUserSignUpResponse(user: User) = repository.registerUser(user)

    fun fetchSocialLoginResponse(providerId: String,name:String, email: String) = repository.verifyUserSocialLogin(providerId, name, email)

    fun updateAccessToken(accessToken: String) = viewModelScope.launch {
        preferencesManager.updateAccessToken(accessToken)
    }
}
