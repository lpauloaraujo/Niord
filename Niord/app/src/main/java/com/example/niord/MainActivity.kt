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
import com.example.niord.CadastroActivity
import com.example.niord.FloatingLifecycleOwner
import com.example.niord.MainOverlayButton
import com.example.niord.Permission

@RequiresApi(Build.VERSION_CODES.O)
class MainActivity : ComponentActivity() {
    private var permission = Permission(this)
    private var lifecycleOwner = FloatingLifecycleOwner().apply {
        onCreate()
        onResume()
    }
    private lateinit var buttonOverlay: MainOverlayButton

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        UserFlowPreferences.ensureDefaults(this)
        enableEdgeToEdge()
        buttonOverlayInit()
        setContentView(R.layout.activity_main)
        setupScreenFlow()
    }

    override fun onDestroy() {
        if (::buttonOverlay.isInitialized) {
            buttonOverlay.onDestroy()
        }
        lifecycleOwner.onDestroy()
        super.onDestroy()
    }

    private fun setupScreenFlow() {
        val splashScreen = findViewById<LinearLayout>(R.id.screenSplash)
        val loginScreen = findViewById<ScrollView>(R.id.screenLogin)

        findViewById<Button>(R.id.btnStart).setOnClickListener {
            if (UserFlowPreferences.shouldShowConfiguration(this)) {
                startActivity(Intent(this, ConfiguracaoActivity::class.java))
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
            UserFlowPreferences.setShowConfiguration(this, true)
            startActivity(Intent(this, ConfiguracaoActivity::class.java))
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
}
