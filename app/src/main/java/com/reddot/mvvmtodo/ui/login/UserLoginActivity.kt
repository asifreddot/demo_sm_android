package com.reddot.mvvmtodo.ui.login

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
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
import com.reddot.mvvmtodo.databinding.ActivityUserLoginActivityBinding
import com.reddot.mvvmtodo.model.SocialAccProviderData
import com.reddot.mvvmtodo.ui.MainActivity
import com.reddot.mvvmtodo.ui.Result
import com.reddot.mvvmtodo.ui.baseClass.BaseActivity
import com.reddot.mvvmtodo.ui.signUp.SignUpActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.activity_user_login_activity.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@AndroidEntryPoint
class UserLoginActivity : BaseActivity() {
    private val TAG = "UserLoginActivity"

    private lateinit var userLoginBinding: ActivityUserLoginActivityBinding
    private lateinit var userLoginViewModel: UserLoginViewModel
    lateinit var mSignInClient: GoogleSignInClient
    private val RC_SIGN_IN = 9001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        userLoginBinding = ActivityUserLoginActivityBinding.inflate(layoutInflater)
        setContentView(userLoginBinding.root)
        userLoginViewModel = ViewModelProvider(this).get(UserLoginViewModel::class.java)

        val googleSignInOptions = GoogleSignInOptions.Builder(
            GoogleSignInOptions.DEFAULT_SIGN_IN
        ).requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        mSignInClient = GoogleSignIn.getClient(this, googleSignInOptions)

        handleUserLogin()

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
                Log.d(TAG, "onActivityResult: ${Gson().toJson(result)}")
                result.getResult(ApiException::class.java)?.let { firebaseAuthWithGoogle(it) }
            } catch (e: Exception) {
                Log.d(TAG, "onActivityResult2: ${e.message}")
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
                    Log.d(TAG, "performSocialLogin: $jsonString")

                    subscribeUiToSocialLogin(prodviderData.uid, prodviderData.displayName,prodviderData.email)
                }
            }
        }
    }

    private fun subscribeUiToSocialLogin(providerId:String, name:String?, email:String?) {
        lifecycleScope.launch(Dispatchers.Main) {
            if (name != null && email!=null) {
                userLoginViewModel.fetchSocialLoginResponse(providerId, name, email).collect { socialLoginResponse ->
                    when {
                        socialLoginResponse.status === Result.Status.LOADING -> {
                            Log.d(TAG, "-------------subscribeUi: LOADING-------------------")
                            showProgressbar()
                        }
                        socialLoginResponse.status == Result.Status.SUCCESS -> {
                            Log.d(TAG, "---------subscribeUi: SUCCESS {$socialLoginResponse}-----------")
                            hideProgressbar()
                            socialLoginResponse?.data?.data?.access_token?.apply {
                                userLoginViewModel.updateAccessToken("Bearer ${socialLoginResponse.data.data.access_token}")
                                Toast.makeText(
                                    this@UserLoginActivity,
                                    "Login Successful",
                                    Toast.LENGTH_LONG
                                ).show()
                                Log.d("checkSocialLogin",Gson().toJson(socialLoginResponse))
                                redirectToActivity(MainActivity::class.java)

                            } ?: run {
                                hideProgressbar()
                                Toast.makeText(
                                    this@UserLoginActivity,
                                    "Login Failed",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                        socialLoginResponse.status == Result.Status.ERROR -> {
                            Log.d(TAG, "-----------subscribeUi: ERROR {$socialLoginResponse}------------")
                            hideProgressbar()
                        }
                    }
                }
            }
        }
    }

    private fun subscribeUiToLogin(email: String, password: String) {
        lifecycleScope.launch(Dispatchers.Main) {
            userLoginViewModel.fetchLoginResponse(email, password).collect { loginResponse ->
                when {
                    loginResponse.status === Result.Status.LOADING -> {
                        Log.d(TAG, "-------------subscribeUi: LOADING-------------------")
                        showProgressbar()
                    }
                    loginResponse.status == Result.Status.SUCCESS -> {
                        Log.d(TAG, "---------subscribeUi: SUCCESS {$loginResponse}-----------")
                        hideProgressbar()
                        loginResponse?.data?.data?.access_token?.apply {
                            userLoginViewModel.updateAccessToken("Bearer ${loginResponse.data.data.access_token}")
                            Toast.makeText(
                                this@UserLoginActivity,
                                "Login Successful",
                                Toast.LENGTH_LONG
                            ).show()
                            redirectToActivity(MainActivity::class.java)

                        } ?: run {
                            hideProgressbar()
                            Toast.makeText(
                                this@UserLoginActivity,
                                "Login Failed",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                    loginResponse.status == Result.Status.ERROR -> {
                        Log.d(TAG, "-----------subscribeUi: ERROR {$loginResponse}------------")
                        hideProgressbar()
                        if(loginResponse.errorCode == 401) {
                            Toast.makeText(
                                this@UserLoginActivity,
                                "Invalid email or password",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            }
        }
    }

    private fun verifyUser(email: String, password: String): Boolean {
        val emailValid = isEmailValid(email)
        val passwordValid = isPasswordValid(password)

        if (email.isNullOrEmpty()) {
            Toast.makeText(this@UserLoginActivity, "Email can't be empty", Toast.LENGTH_LONG).show()
        }
        else if (!emailValid) {
            Toast.makeText(this@UserLoginActivity, "Invalid email", Toast.LENGTH_LONG).show()
        }

        else if (password.isNullOrEmpty()) {
            Toast.makeText(this@UserLoginActivity, "Password can't be empty", Toast.LENGTH_LONG).show()
        }
        else if (!passwordValid) {
            Toast.makeText(this@UserLoginActivity, "Invalid password", Toast.LENGTH_LONG).show()
        }

        return emailValid && passwordValid
    }

    private fun isEmailValid(email: String?): Boolean {
        return !email.isNullOrEmpty() && android.util.Patterns.EMAIL_ADDRESS.matcher(email)
            .matches()
    }

    private fun isPasswordValid(password: String?): Boolean {
        return !password.isNullOrEmpty() && password.length >= 4
    }

    private fun handleUserLogin() {
        userLoginBinding.apply {

            tvSignUp.setOnClickListener {
                redirectToActivity(SignUpActivity::class.java)
            }

            ivGmailLogin.setOnClickListener {
                performGoogleSignIn()
            }

            btnUserLogin.setOnClickListener {
                val email = et_email.text.toString()
                val password = et_password.text.toString()

                if(verifyUser(email, password)) {
                    subscribeUiToLogin(email, password)
                }

            }

        }
    }

    private fun showProgressbar() {
        try {
            userLoginBinding.progressbarLogin.visibility = View.VISIBLE
        } catch (e:Exception) {
            e.printStackTrace()
        }
    }

    private fun hideProgressbar() {
        try {
            userLoginBinding.progressbarLogin.visibility = View.GONE
        } catch (e:Exception) {
            e.printStackTrace()
        }
    }
}