package com.reddot.mvvmtodo.ui.signUp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.GoogleAuthProvider
import com.google.gson.Gson
import com.reddot.mvvmtodo.R
import com.reddot.mvvmtodo.databinding.ActivitySignUpBinding
import com.reddot.mvvmtodo.model.SocialAccProviderData
import com.reddot.mvvmtodo.ui.MainActivity
import com.reddot.mvvmtodo.ui.Result
import com.reddot.mvvmtodo.ui.baseClass.BaseActivity
import com.reddot.mvvmtodo.ui.login.UserLoginActivity
import com.reddot.mvvmtodo.utility.AppUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SignUpActivity : BaseActivity() {
    private val TAG = "SignUpActivity"
    private lateinit var binding: ActivitySignUpBinding
    private lateinit var viewModel: SignUpViewModel
    lateinit var mSignInClient: GoogleSignInClient
    private val RC_SIGN_IN = 9001


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_sign_up)
        viewModel = ViewModelProvider(this).get(SignUpViewModel::class.java)
        binding.viewModel = viewModel

        val googleSignInOptions = GoogleSignInOptions.Builder(
            GoogleSignInOptions.DEFAULT_SIGN_IN
        ).requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        mSignInClient = GoogleSignIn.getClient(this, googleSignInOptions)
        initListener()
    }


    private fun initListener() {
        binding.apply {
            btnSubmit.setOnClickListener { handleSubmitButton() }
            tvLogin.setOnClickListener {
                redirectToActivity(UserLoginActivity::class.java)
            }
            llSignUpGoogle.setOnClickListener {
                performGoogleSignIn()
            }
        }

    }

    private fun verifyUser(): Boolean {
        val emailValid = isEmailValid(viewModel.email.get())
        val passwordValid = isPasswordValid(viewModel.password.get())
        val confirmPasswordValid = isPasswordValid(viewModel.confirmPassword.get())

        if (viewModel.email.get().isNullOrEmpty()) {
            Toast.makeText(this@SignUpActivity, "Email can't be empty", Toast.LENGTH_LONG).show()
        } else if (!emailValid) {
            Toast.makeText(this@SignUpActivity, "Invalid email", Toast.LENGTH_LONG).show()
        } else if (viewModel.phone.get().isNullOrEmpty()) {
            Toast.makeText(this@SignUpActivity, "Phone number can't be empty", Toast.LENGTH_LONG)
                .show()
        }
        else if (viewModel.password.get().isNullOrEmpty()) {
            Toast.makeText(this@SignUpActivity, "Password can't be empty", Toast.LENGTH_LONG).show()
        } else if (!passwordValid) {
            Toast.makeText(this@SignUpActivity, "Invalid password", Toast.LENGTH_LONG).show()
        } else if (viewModel.confirmPassword.get().isNullOrEmpty()) {
            Toast.makeText(
                this@SignUpActivity,
                "Confirm password can't be empty",
                Toast.LENGTH_LONG
            ).show()
        } else if (!confirmPasswordValid) {
            Toast.makeText(this@SignUpActivity, "Invalid confirm password", Toast.LENGTH_LONG)
                .show()
        } else if (!viewModel.password.get().equals(viewModel.confirmPassword.get())) {
            Toast.makeText(
                this@SignUpActivity,
                "Confirm password doesn't match with password",
                Toast.LENGTH_LONG
            ).show()
        }
        val isPhoneNumberMatched = viewModel.password.get().equals(viewModel.confirmPassword.get())

        return emailValid && passwordValid && confirmPasswordValid && isPhoneNumberMatched
    }

    private fun isEmailValid(email: String?): Boolean {
        return !email.isNullOrEmpty() && android.util.Patterns.EMAIL_ADDRESS.matcher(email)
            .matches()
    }

    private fun isPasswordValid(password: String?): Boolean {
        return !password.isNullOrEmpty() && password.length >= 4
    }

    private fun getUser(): User {
        val user = User()
        user.email = viewModel.email.get()
        user.name = AppUtils.extractUsernameFromEmail(user.email)
        user.password = viewModel.password.get()
        user.phone = viewModel.phone.get()
        return user
    }

    private fun handleSubmitButton() {
        if (verifyUser()) {
            subscribeUiToRegister()
        }
    }

    private fun performGoogleSignIn() {
        try {
            mFirebaseAuth.signOut()
            mSignInClient.signOut()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val intent = mSignInClient.signInIntent
        startActivityForResult(intent, RC_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            try {
                val result = GoogleSignIn.getSignedInAccountFromIntent(data)
                Log.d(TAG, "onActivityResult: ${result}")
                result.getResult(ApiException::class.java)?.let { firebaseAuthWithGoogle(it) }
            } catch (e: Exception) {
                Log.d(TAG, "onActivityResult: ${e.message}")
                e.printStackTrace()
            }
        }

    }

    private fun firebaseAuthWithGoogle(acct: GoogleSignInAccount) {
        val credential = GoogleAuthProvider.getCredential(acct.idToken, null)
        mFirebaseAuth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    fetchSocialLoginUserIdToken("google", task)
                } else {
                    Log.w(TAG, "firebaseAuthWithGoogle:failure", task.exception)
                }
            }
            .addOnFailureListener(this) { e ->
                Toast.makeText(
                    this, "Authentication failed.",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun fetchSocialLoginUserIdToken(type: String, task: Task<AuthResult>) {
        Log.d(TAG, "firebaseAuthWith  $type : success ${task.result} ")

        mFirebaseAuth.currentUser?.getIdToken(true)?.addOnCompleteListener {

            try {
                if (it.isSuccessful) {
                    Log.d(TAG, "finalSocialResult: token: ${it.result?.token}")
                    verifySocialLoginWithAppServer(task, it.result?.token)
                } else {
                    Log.d(TAG, "finalSocialResult: token: token fetch failed")
                }
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }
        }
    }

    @Throws(Exception::class)
    private fun verifySocialLoginWithAppServer(task: Task<AuthResult>, token: String?) {

        token?.let { token ->
            task.result.user?.providerData?.let {

                it[1].let { provider ->
                    val prodviderData = SocialAccProviderData(
                        providerId = provider.providerId,
                        uid = provider.uid,
                        displayName = provider.displayName,
                        email = provider.email,
                        phoneNumber = provider.phoneNumber,
                        photoURL = provider.photoUrl.toString()
                    )
                    val jsonString = Gson().toJson(prodviderData)
                    Log.d(TAG, "performSocialLogin: ${jsonString}")

                    subscribeUiToSocialRegister(
                        prodviderData.uid,
                        prodviderData.displayName,
                        prodviderData.email
                    )

                }
            }
        }
    }

    private fun subscribeUiToSocialRegister(providerId: String, name: String?, email: String?) {
        lifecycleScope.launch(Dispatchers.Main) {
            if (name != null && email != null) {
                viewModel.fetchSocialLoginResponse(providerId, name, email)
                    .collect { socialRegisterResponse ->
                        when {
                            socialRegisterResponse.status === Result.Status.LOADING -> {
                                Log.d(TAG, "-------------subscribeUi: LOADING-------------------")
                                showProgressbar()
                            }
                            socialRegisterResponse.status == Result.Status.SUCCESS -> {
                                Log.d(
                                    TAG,
                                    "---------subscribeUi: SUCCESS {$socialRegisterResponse}-----------"
                                )
                                hideProgressbar()
                                socialRegisterResponse?.data?.data?.access_token?.apply {
                                    viewModel.updateAccessToken("Bearer ${socialRegisterResponse.data.data.access_token}")
//                                    Toast.makeText(
//                                        this@SignUpActivity,
//                                        "Registration Successful",
//                                        Toast.LENGTH_LONG
//                                    ).show()
                                    redirectToActivity(MainActivity::class.java)

                                } ?: run {
                                    Toast.makeText(
                                        this@SignUpActivity,
                                        "Registration Failed",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                            socialRegisterResponse.status == Result.Status.ERROR -> {
                                Log.d(
                                    TAG,
                                    "-----------subscribeUi: ERROR {$socialRegisterResponse}------------"
                                )
                                hideProgressbar()
                                Log.d(
                                    "checkSocialLoginError",
                                    Gson().toJson(socialRegisterResponse.errorCode)
                                )
                            }
                        }
                    }
            }
        }
    }

    private fun subscribeUiToRegister() {
        lifecycleScope.launch(Dispatchers.Main) {
            viewModel.fetchUserSignUpResponse(getUser()).collect { response ->
                when {
                    response.status === Result.Status.LOADING -> {
                        Log.d(TAG, "-------------subscribeUi: LOADING-------------------")
                        showProgressbar()
                    }
                    response.status == Result.Status.SUCCESS -> {
                        Log.d(TAG, "---------subscribeUi: SUCCESS {$response}-----------")
                        hideProgressbar()
                        Log.d("checkSignUpResponse", Gson().toJson(response))
//                        Toast.makeText(
//                            this@SignUpActivity,
//                            "User Created Successfully",
//                            Toast.LENGTH_LONG
//                        ).show()
                        redirectToActivity(UserLoginActivity::class.java)

                    }
                    response.status == Result.Status.ERROR -> {
                        Log.d(TAG, "-----------subscribeUi: ERROR {$response}------------")
                        hideProgressbar()
                    }
                }

            }
        }
    }

    private fun showProgressbar() {
        try {
            binding.progressbarSignUp.visibility = View.VISIBLE
        } catch (e:Exception) {
            e.printStackTrace()
        }
    }

    private fun hideProgressbar() {
        try {
            binding.progressbarSignUp.visibility = View.GONE
        } catch (e:Exception) {
            e.printStackTrace()
        }
    }
}
