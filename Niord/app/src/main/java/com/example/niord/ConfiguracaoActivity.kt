package com.example.niord

import android.Manifest
import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
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
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.niord.api.ApiService
import com.example.niord.api.ErrorResponse
import com.example.niord.api.User
import io.ktor.client.call.body
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.O)
class ConfiguracaoActivity : ComponentActivity() {
    private var permission = Permission(this)
    private var callMonitor: CallMonitor? = null

    private var lifecycleOwner = FloatingLifecycleOwner().apply {
        onCreate()
        onResume()
    }

    private lateinit var apiService: ApiService
    private lateinit var buttonOverlay: MainOverlayButton
    private lateinit var locationManager: LocationManager

    private val overlayReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (::buttonOverlay.isInitialized) {
                val isEnabled = UserFlowPreferences.isOverlayEnabled(this@ConfiguracaoActivity)
                applyOverlayEnabledState(isEnabled, false)
            }
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


        locationManager = LocationManager(this)

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
    }

    override fun onResume() {
        super.onResume()
        if (::buttonOverlay.isInitialized) {
            buttonOverlay.onDestroy()
        }
        buttonOverlayInit()
        
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
        if (::buttonOverlay.isInitialized) {
            buttonOverlay.onDestroy()
        }
        lifecycleOwner.onDestroy()
        super.onDestroy()
    }

    private fun buttonOverlayInit() {
        buttonOverlay = MainOverlayButton(this, lifecycleOwner)
        buttonOverlay.isDraggable = !UserFlowPreferences.isOverlayLocked(this)
        buttonOverlay.statePacket.vigiaActive = VigiaService.isRunning
        buttonOverlay.setVisibility(false)

        buttonOverlay.onCallClick = { number ->
            if(permission.isCallPermitted(this)) {
                showCallDialog(number)
            }
        }

        buttonOverlay.onVigiaClick = { isActive ->
            showVigiaDialog(isActive)
        }

        buttonOverlay.onLocationClick = {printUserLocation()}
    }

    private fun setupControls() {
        val checkboxDesativar = findViewById<CheckBox>(R.id.checkboxDesativar)
        val switchFixar = findViewById<SwitchCompat>(R.id.switchFixar)
        val itemDesativar = findViewById<RelativeLayout>(R.id.itemDesativarBotao)
        val itemFixar = findViewById<RelativeLayout>(R.id.itemFixarBotao)
        val itemLogout = findViewById<RelativeLayout>(R.id.itemLogout)
        val itemExcluirConta = findViewById<RelativeLayout>(R.id.itemExcluirConta)
        val itemAlterarDados = findViewById<RelativeLayout>(R.id.itemAlterarDados)
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
    }


    fun printUserLocation() {

        locationManager.getUserLocation { location ->

            if (location != null) {

                Log.d(
                    "LOCATION",
                    "Lat: ${location.latitude}, Lng: ${location.longitude}"
                )

            } else {

                Log.d("LOCATION", "Sem localização")
            }
        }
    }

    private fun openAccountSecurityFlow() {
        startActivity(Intent(this, AccountSecurityActivity::class.java))
    }

    private fun applyOverlayEnabledState(
        enabled: Boolean,
        requestPermissionIfNeeded: Boolean
    ) {
        if (!enabled) {
            UserFlowPreferences.setOverlayEnabled(this, false)
            if (::buttonOverlay.isInitialized) {
                buttonOverlay.setVisibility(false)
                buttonOverlay.dismiss()
            }
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
        buttonOverlay.invoke()
        buttonOverlay.setVisibility(true)
        updateFixControlState(true)
    }

    private fun applyOverlayLockedState(locked: Boolean) {
        UserFlowPreferences.setOverlayLocked(this, locked)
        if (::buttonOverlay.isInitialized) {
            buttonOverlay.isDraggable = !locked
        }
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

    private fun showVigiaDialog(isActive: Boolean) {
        if (isActive) {
            showVigiaDeactivateDialog()
        } else {
            showVigiaActivateDialog()
        }
    }

    private fun showVigiaActivateDialog() {
        val dialog = com.google.android.material.dialog.MaterialAlertDialogBuilder(
            this,
            R.style.CustomAlertDialog
        )
            .setTitle("Ativar Niord Vigia?")
            .setMessage(
                "O app vai monitorar o áudio do seu aparelho em segundo plano para identificar " +
                    "ameaças, brigas ou comportamentos perigosos."
            )
            .setPositiveButton("Ativar Proteção") { _, _ -> startVigia() }
            .setNegativeButton("Cancelar", null)
            .create()
        dialog.window?.setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)
        dialog.show()
    }

    private fun showVigiaDeactivateDialog() {
        val dialog = com.google.android.material.dialog.MaterialAlertDialogBuilder(
            this,
            R.style.CustomAlertDialog
        )
            .setTitle("Desativar Niord Vigia?")
            .setMessage("O monitoramento de áudio em segundo plano será encerrado.")
            .setPositiveButton("Desativar") { _, _ -> stopVigia() }
            .setNegativeButton("Cancelar", null)
            .create()
        dialog.window?.setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)
        dialog.show()
    }

    private fun showVigiaActivatedDialog() {
        val dialog = com.google.android.material.dialog.MaterialAlertDialogBuilder(
            this,
            R.style.CustomAlertDialog
        )
            .setTitle("Niord Vigia Ativado")
            .setMessage("O monitoramento de áudio está rodando em segundo plano.")
            .setPositiveButton("Entendi", null)
            .create()
        dialog.window?.setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)
        dialog.show()
    }

    private fun startVigia() {
        if (!permission.isVigiaPermitted(this)) {
            permission.requestVigiaPermissions { granted ->
                if (granted) startVigiaService()
            }
            return
        }
        startVigiaService()
    }

    private fun startVigiaService() {
        VigiaService.start(this)
        UserFlowPreferences.setVigiaActive(this, true)
        buttonOverlay.statePacket.vigiaActive = true
        showVigiaActivatedDialog()
    }

    private fun stopVigia() {
        VigiaService.stop(this)
        UserFlowPreferences.setVigiaActive(this, false)
        buttonOverlay.statePacket.vigiaActive = false
    }

    private fun showCallDialog(number: String) {

        val title: String
        val message: String
        val positiveText: String
        val negativeText: String

        when (number) {

            "144" -> {
                title = "Ligar para Emergência?"
                message = "Você será direcionado para a chamada telefônica. Confirme para discar imediatamente."
                positiveText = "Ligar Agora"
                negativeText = "Cancelar"
            }

            "1052" -> {
                title = "Ligar para a Polícia?"
                message = "Você será direcionado para a chamada telefônica. Confirme para discar imediatamente."
                positiveText = "Ligar Agora"
                negativeText = "Cancelar"
            }

            else -> {
                title = "Chamada"
                message = "Deseja realmente ligar para $number?"
                positiveText = "Confirmar"
                negativeText = "Cancelar"
            }
        }

        val dialog = com.google.android.material.dialog.MaterialAlertDialogBuilder(
            this,
            R.style.CustomAlertDialog
        )
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(positiveText) { _, _ ->

                        // 🔹 cria o monitor
                        callMonitor = CallMonitor(
                            context = this,
                            onCallStarted = {
                                runOnUiThread {
                                }
                            },
                            onCallEnded = {
                                runOnUiThread {

                                    callMonitor?.stop()
                                    callMonitor = null

                                    if (number == "144") {
                                        runOnUiThread {
                                            val intent = Intent(this, PosEmergenciaActivity::class.java)
                                            startActivity(intent)
                                        }
                                    } else if (number == "1052") {
                                        runOnUiThread {
                                            val intent = Intent(this, PosPoliciaActivity::class.java)
                                            startActivity(intent)
                                        }
                                    }
                                }
                            }
                        )

                        callMonitor?.start()
                        CallManager().toCall(this, number)

            }
            .setNegativeButton(negativeText, null)
            .create()
        dialog.window?.setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)
        dialog.show()
    }

}
