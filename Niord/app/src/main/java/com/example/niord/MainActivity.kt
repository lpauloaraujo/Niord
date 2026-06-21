package com.example.niord

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.example.niord.api.ApiService
import com.example.niord.api.LoginPost
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.O)
class MainActivity : ComponentActivity() {
    private var permission = Permission(this)
    private lateinit var apiService: ApiService

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        UserFlowPreferences.ensureDefaults(this)
        DebugPreferences.ensureDefaults(this)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        findViewById<ScrollView>(R.id.screenLogin).applyStatusBarPadding()
        setupScreenFlow()

        apiService = ApiService(this)
    }


    suspend fun sendLogin(): Boolean{
        val emailField = findViewById<TextView>(R.id.editEmailCpf)
        val passwordField = findViewById<TextView>(R.id.editSenhaLogin)
        val email = emailField.text.toString().trim()
        val password = passwordField.text.toString()
        var valid = true

        if (email.isBlank()) {
            emailField.error = "Email obrigatório"
            valid = false
        }
        if (password.isBlank()) {
            passwordField.error = "Senha obrigatória"
            valid = false
        }
        if (!valid) return false

        val loginData = LoginPost(
            email = email,
            password = password
        )
        try {
            val response = apiService.sendLoginData(loginData)
            if(response.status.value == 200) {
                return true
            }else{
                val credentialsMessage = "Credenciais Inválidas"
                passwordField.error = credentialsMessage
                emailField.error = credentialsMessage
            }
        }catch(e: Exception){
            passwordField.error = "Não foi possível conectar ao servidor"
        }

        return false
    }

    suspend fun isLoggedIn(): Boolean{
        try{
            //Verifies response from auth protected endpoint
            val response = apiService.isAuth()
            if(response.status.value == 200) return true
        }catch (e: Exception){
            Log.e("AUTH_ERR", e.toString())
        }
        return false
    }

    private fun setupScreenFlow() {
        val splashScreen = findViewById<LinearLayout>(R.id.screenSplash)
        val loginScreen = findViewById<ScrollView>(R.id.screenLogin)


        findViewById<Button>(R.id.btnStart).setOnClickListener {
            if (!DebugPreferences.isDebug(this)) {
                lifecycleScope.launch {
                    if (isLoggedIn()) {
                        openPostAuthFlow()
                    } else {
                        splashScreen.visibility = View.GONE
                        loginScreen.visibility = View.VISIBLE
                    }
                }

            } else {
                splashScreen.visibility = View.GONE
                loginScreen.visibility = View.VISIBLE
            }
        }

        findViewById<ImageButton>(R.id.btnBackToSplash).setOnClickListener {
            splashScreen.visibility = View.VISIBLE
            loginScreen.visibility = View.GONE
        }

        findViewById<Button>(R.id.btnLogin).setOnClickListener {
            if(!DebugPreferences.isDebug(this)) {
                lifecycleScope.launch {
                    if(sendLogin()) {
                        openPostAuthFlow()
                    }
                }
            }else{
                openPostAuthFlow()
            }
        }

        findViewById<TextView>(R.id.btnCreateAccount).setOnClickListener {
            startActivity(Intent(this, CadastroActivity::class.java))
        }
    }


    private fun openPostAuthFlow() {
        val nextActivity = if (UserFlowPreferences.shouldShowOnboarding(this)) {
            OnboardingActivity::class.java
        } else {
            ConfiguracaoActivity::class.java
        }
        startActivity(Intent(this, nextActivity))
    }
}
