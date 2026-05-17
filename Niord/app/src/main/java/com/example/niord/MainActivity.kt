package com.example.niord

import android.content.Intent
import android.os.Build
import android.os.Bundle
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
import com.example.niord.CadastroActivity
import com.example.niord.FloatingLifecycleOwner
import com.example.niord.MainOverlayButton
import com.example.niord.Permission
import com.example.niord.api.ApiService
import com.example.niord.api.LoginPost
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.O)
class MainActivity : ComponentActivity() {
    private var permission = Permission(this)
    private var lifecycleOwner = FloatingLifecycleOwner().apply {
        onCreate()
        onResume()
    }
    private lateinit var buttonOverlay: MainOverlayButton
    private lateinit var apiService: ApiService

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        UserFlowPreferences.ensureDefaults(this)
        DebugPreferences.ensureDefaults(this)
        enableEdgeToEdge()
        buttonOverlayInit()
        setContentView(R.layout.activity_main)
        findViewById<ScrollView>(R.id.screenLogin).applyStatusBarPadding()
        setupScreenFlow()

        apiService = ApiService(this)
    }

    override fun onDestroy() {
        if (::buttonOverlay.isInitialized) {
            buttonOverlay.onDestroy()
        }
        lifecycleOwner.onDestroy()
        super.onDestroy()
    }

    suspend fun sendLogin(): Boolean{
        val loginData = LoginPost(
            email=findViewById<TextView>(R.id.editEmailCpf).text.toString(),
            password = findViewById<TextView>(R.id.editSenhaLogin).text.toString()
        )
        try {
            val response = apiService.sendLoginData(loginData)
            if(response.status.value == 200) {
                return true
            }else{
                val credentialsMessage = "Credenciais Inválidas"
                findViewById<TextView>(R.id.editSenhaLogin).error = credentialsMessage
                findViewById<TextView>(R.id.editEmailCpf).error = credentialsMessage
            }
        }catch(e: Exception){}

        return false
    }

    suspend fun isLoggedIn(): Boolean{
        try{
            //Verifies response from auth protected endpoint
            val response = apiService.isAuth()
            if(response.status.value == 200) return true
        }catch (e: Exception){}
        return false
    }

    private fun setupScreenFlow() {
        val splashScreen = findViewById<LinearLayout>(R.id.screenSplash)
        val loginScreen = findViewById<ScrollView>(R.id.screenLogin)


        findViewById<Button>(R.id.btnStart).setOnClickListener {
            if (UserFlowPreferences.shouldShowConfiguration(this)) {
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

    fun buttonOverlayInit(){
        buttonOverlay = MainOverlayButton(this, lifecycleOwner)
        buttonOverlay.setVisibility(false)
        buttonOverlay.invoke()
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
