package com.example.niord

import android.Manifest
import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.view.WindowManager
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.RelativeLayout
import androidx.annotation.RequiresApi
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.widget.SwitchCompat
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import com.example.niord.api.ApiService
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.O)
class ConfiguracaoActivity : ComponentActivity() {
    private var permission = Permission(this)

    private lateinit var apiService: ApiService

    private val overlayReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val isEnabled = UserFlowPreferences.isOverlayEnabled(this@ConfiguracaoActivity)
            if(isEnabled){
                mService?.setVisibility(true)
                mService?.refresh()
            }
        }
    }

    private var mService: FloatingOverlayService? = null
    private var mBound = false

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as FloatingOverlayService.LocalBinder
            mService = binder.getService()
            mBound = true
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            mBound = false
            mService = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.configuracao)

        val filter = IntentFilter("com.example.niord.UPDATE_OVERLAY")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(overlayReceiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            registerReceiver(overlayReceiver, filter)
        }


        findViewById<android.view.View>(R.id.main).applyStatusBarPadding()
        setupControls()

        findViewById<ImageButton>(R.id.btnVoltar).setOnClickListener {
            finish()
        }


        UserFlowPreferences.setShowConfiguration(this, true)

        apiService = ApiService(this)

        if (!permission.isCallPermitted(this)){
            permission.requestCallAndPhoneStatePermission {  }
            ActivityCompat.shouldShowRequestPermissionRationale(
                this, Manifest.permission.CALL_PHONE)
        }

        if(!permission.isVigiaPermitted(this)){
            permission.requestVigiaPermissions{}
        }
        if(!permission.isSmsPermitted(this)){
           permission.requestSmsPermission{}
        }
        if(!permission.isLocationPermitted(this)) {
            permission.requestLocationPermission {}
        }

    }

    override fun onResume() {
        super.onResume()
        /*
        if (::buttonOverlay.isInitialized) {
            buttonOverlay.onDestroy()
        }
        buttonOverlayInit()*/

        //Intent(this, FloatingOverlayService::class.java).also { intent ->
         //   bindService(intent, connection, Context.BIND_AUTO_CREATE)
        //}

        // Sincroniza sem disparar listeners
        syncControlsWithPreferences()

        val isEnabled = UserFlowPreferences.isOverlayEnabled(this)
        applyOverlayEnabledState(isEnabled, requestPermissionIfNeeded = false)
    }

    override fun onDestroy() {
        try {
            unregisterReceiver(overlayReceiver)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        if (mBound) {
            unbindService(connection)
            mBound = false
        }
        super.onDestroy()
    }

    private fun startOverlay(){
        //permission.getOverlayPermissions{}
        if (Settings.canDrawOverlays(this)) {
            val intent = Intent(this, FloatingOverlayService::class.java)
            this.startForegroundService(intent) // or startService(intent) on older Android versions

            //Initiates Binder
            Intent(this, FloatingOverlayService::class.java).also { intent ->
               bindService(intent, connection, Context.BIND_AUTO_CREATE)
            }
        }


    }

    private fun endOverlay(){
        if (mBound) {
            unbindService(connection)
            mBound = false
        }
        val intent = Intent(this, FloatingOverlayService::class.java)
        this.stopService(intent)
    }


    private fun setupControls() {
        val checkboxDesativar = findViewById<CheckBox>(R.id.checkboxDesativar)
        val switchFixar = findViewById<SwitchCompat>(R.id.switchFixar)
        val itemDesativar = findViewById<RelativeLayout>(R.id.itemDesativarBotao)
        val itemFixar = findViewById<RelativeLayout>(R.id.itemFixarBotao)
        val itemLogout = findViewById<RelativeLayout>(R.id.itemLogout)
        val itemExcluirConta = findViewById<RelativeLayout>(R.id.itemExcluirConta)
        val itemAlterarDados = findViewById<RelativeLayout>(R.id.itemAlterarDados)
        val itemContatosEmergencia = findViewById<RelativeLayout>(R.id.itemContatosEmergencia)
        val itemPersonalizar = findViewById<RelativeLayout>(R.id.itemPersonalizarBotao)

        // Listeners apenas para interações do usuário
        checkboxDesativar.setOnClickListener {
            val isChecked = (it as CheckBox).isChecked
            applyOverlayEnabledState(isChecked, requestPermissionIfNeeded = true)
        }

        switchFixar.setOnClickListener {
            val isChecked = (it as SwitchCompat).isChecked
            applyOverlayLockedState(isChecked)
        }

        itemDesativar.setOnClickListener {
            checkboxDesativar.isChecked = !checkboxDesativar.isChecked
            applyOverlayEnabledState(checkboxDesativar.isChecked, requestPermissionIfNeeded = true)
        }

        itemFixar.setOnClickListener {
            if (switchFixar.isEnabled) {
                switchFixar.isChecked = !switchFixar.isChecked
                applyOverlayLockedState(switchFixar.isChecked)
            }
        }

        itemPersonalizar.setOnClickListener {
            mService?.setVisibility(false)
            startActivity(Intent(this, FloatingButtonCustomizationActivity::class.java))
        }

        itemLogout.setOnClickListener {
            showLogoutDialog()
        }

        itemExcluirConta.setOnClickListener {
            showDeleteAccountDialog()
        }

        itemAlterarDados.setOnClickListener {
            openAccountSecurityFlow()
        }

        itemContatosEmergencia.setOnClickListener {
            openContatosEmergenciaFlow()
        }
    }


    private fun openAccountSecurityFlow() {
        startActivity(Intent(this, AccountSecurityActivity::class.java))
    }

    private fun openContatosEmergenciaFlow() {
        startActivity(Intent(this, ContatosEmergenciaActivity::class.java))
    }

    private fun applyOverlayEnabledState(
        enabled: Boolean,
        requestPermissionIfNeeded: Boolean
    ) {
        if (!enabled) {
            UserFlowPreferences.setOverlayEnabled(this, false)
            endOverlay()
            updateFixControlState(false)
            return
        }

        if (!Settings.canDrawOverlays(this)) {
            if (requestPermissionIfNeeded) {
                permission.getOverlayPermissions {
                    val granted = Settings.canDrawOverlays(this)
                    UserFlowPreferences.setOverlayEnabled(this, granted)
                    // No UI call here, onResume will handle it
                    syncControlsWithPreferences()
                }
            } else {
                UserFlowPreferences.setOverlayEnabled(this, false)
                updateFixControlState(false)
            }
            return
        }

        UserFlowPreferences.setOverlayEnabled(this, true)
        startOverlay()
        updateFixControlState(true)
    }

    private fun applyOverlayLockedState(locked: Boolean) {
        UserFlowPreferences.setOverlayLocked(this, locked)
        mService?.fixOverlay(locked)
    }

    private fun syncControlsWithPreferences() {
        val isEnabled = UserFlowPreferences.isOverlayEnabled(this)
        val isLocked = UserFlowPreferences.isOverlayLocked(this)
        
        findViewById<CheckBox>(R.id.checkboxDesativar).isChecked = isEnabled
        findViewById<SwitchCompat>(R.id.switchFixar).isChecked = isLocked
        updateFixControlState(isEnabled)
    }

    private fun updateFixControlState(isOverlayEnabled: Boolean) {
        val switchFixar = findViewById<SwitchCompat>(R.id.switchFixar)
        val itemFixar = findViewById<RelativeLayout>(R.id.itemFixarBotao)

        switchFixar.isEnabled = isOverlayEnabled
        switchFixar.isClickable = isOverlayEnabled
        switchFixar.alpha = if (isOverlayEnabled) 1f else 0.45f
        itemFixar.alpha = if (isOverlayEnabled) 1f else 0.45f
    }

    private suspend fun sendLogoutData(): Boolean{
        try {
            val response = apiService.logout()
            if(response.status.value == 200) return true
            if(response.status.value == 422) {
                val errorMessage = response.bodyAsText()
                println(errorMessage)
            }
        }catch(e: Exception){}
        return false
    }

    private fun showLogoutDialog() {
        val dialog = AlertDialog.Builder(this)
            .setTitle("Confirmar Logout")
            .setMessage("Tem certeza que deseja fazer logout?")
            .setPositiveButton("Confirmar") { dialogInterface, _ ->
                UserFlowPreferences.setShowConfiguration(this, false)
                val intent = Intent(this, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                }
                lifecycleScope.launch {
                    sendLogoutData()
                    startActivity(intent)
                    finish()
                }

            }
            .setNegativeButton("Cancelar") { dialogInterface, _ ->
                dialogInterface.dismiss()
            }
            .create()

        dialog.show()

        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.apply {
            setTextColor(android.graphics.Color.parseColor("#4A6CF7"))
            textSize = 16f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }

        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.apply {
            setTextColor(android.graphics.Color.parseColor("#666666"))
            textSize = 16f
        }
    }

    private fun showDeleteAccountDialog() {
        val dialog = AlertDialog.Builder(this)
            .setTitle("Excluir Conta")
            .setMessage("Tem certeza que deseja excluir sua conta?")
            .setPositiveButton("Confirmar") { _, _ ->
                UserFlowPreferences.setShowConfiguration(this, false)
                UserFlowPreferences.setOnboardingCompleted(this, false)
                UserFlowPreferences.setOverlayEnabled(this, false)
                UserFlowPreferences.setOverlayLocked(this, false)
                val intent = Intent(this, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                }
                startActivity(intent)
                finish()
            }
            .setNegativeButton("Cancelar") { dialogInterface, _ ->
                dialogInterface.dismiss()
            }
            .create()

        dialog.show()

        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.apply {
            setTextColor(android.graphics.Color.parseColor("#4A6CF7"))
            textSize = 16f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }

        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.apply {
            setTextColor(android.graphics.Color.parseColor("#666666"))
            textSize = 16f
        }
    }

}
