package com.example.niord

import android.app.AlertDialog
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.RelativeLayout
import androidx.annotation.RequiresApi
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.widget.SwitchCompat

@RequiresApi(Build.VERSION_CODES.O)
class ConfiguracaoActivity : ComponentActivity() {
    private var permission = Permission(this)
    private var lifecycleOwner = FloatingLifecycleOwner().apply {
        onCreate()
        onResume()
    }
    private lateinit var buttonOverlay: MainOverlayButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        buttonOverlayInit()
        setContentView(R.layout.configuracao)
        setupControls()

        findViewById<ImageButton>(R.id.btnVoltar).setOnClickListener {
            finish()
        }
    }

    override fun onDestroy() {
        if (::buttonOverlay.isInitialized) {
            buttonOverlay.onDestroy()
        }
        lifecycleOwner.onDestroy()
        super.onDestroy()
    }

    private fun buttonOverlayInit() {
        buttonOverlay = MainOverlayButton(this, lifecycleOwner)
        buttonOverlay.isDraggable = !UserFlowPreferences.isOverlayLocked(this)
        buttonOverlay.setVisibility(false)
    }

    private fun setupControls() {
        val checkboxDesativar = findViewById<CheckBox>(R.id.checkboxDesativar)
        val switchFixar = findViewById<SwitchCompat>(R.id.switchFixar)
        val itemDesativar = findViewById<RelativeLayout>(R.id.itemDesativarBotao)
        val itemFixar = findViewById<RelativeLayout>(R.id.itemFixarBotao)
        val itemLogout = findViewById<RelativeLayout>(R.id.itemLogout)
        val itemExcluirConta = findViewById<RelativeLayout>(R.id.itemExcluirConta)

        checkboxDesativar.isChecked = UserFlowPreferences.isOverlayEnabled(this)
        switchFixar.isChecked = UserFlowPreferences.isOverlayLocked(this)

        applyOverlayEnabledState(checkboxDesativar.isChecked, requestPermissionIfNeeded = false)
        applyOverlayLockedState(switchFixar.isChecked)

        checkboxDesativar.setOnCheckedChangeListener { _, isChecked ->
            applyOverlayEnabledState(isChecked, requestPermissionIfNeeded = true)
        }

        switchFixar.setOnCheckedChangeListener { _, isChecked ->
            applyOverlayLockedState(isChecked)
        }

        itemDesativar.setOnClickListener {
            checkboxDesativar.isChecked = !checkboxDesativar.isChecked
        }

        itemFixar.setOnClickListener {
            if (switchFixar.isEnabled) {
                switchFixar.isChecked = !switchFixar.isChecked
            }
        }

        itemLogout.setOnClickListener {
            showLogoutDialog()
        }

        itemExcluirConta.setOnClickListener {
            showDeleteAccountDialog()
        }
    }

    private fun applyOverlayEnabledState(
        enabled: Boolean,
        requestPermissionIfNeeded: Boolean
    ) {
        if (!enabled) {
            UserFlowPreferences.setOverlayEnabled(this, false)
            if (::buttonOverlay.isInitialized) {
                buttonOverlay.setVisibility(false)
            }
            updateFixControlState(false)
            return
        }

        if (!Settings.canDrawOverlays(this)) {
            if (requestPermissionIfNeeded) {
                permission.getOverlayPermissions {
                    val granted = Settings.canDrawOverlays(this)
                    UserFlowPreferences.setOverlayEnabled(this, granted)
                    if (granted) {
                        buttonOverlay.invoke()
                        buttonOverlay.setVisibility(true)
                    }
                    updateFixControlState(granted)
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
        findViewById<CheckBox>(R.id.checkboxDesativar).isChecked =
            UserFlowPreferences.isOverlayEnabled(this)
        findViewById<SwitchCompat>(R.id.switchFixar).isChecked =
            UserFlowPreferences.isOverlayLocked(this)
        updateFixControlState(UserFlowPreferences.isOverlayEnabled(this))
    }

    private fun updateFixControlState(isOverlayEnabled: Boolean) {
        val switchFixar = findViewById<SwitchCompat>(R.id.switchFixar)
        val itemFixar = findViewById<RelativeLayout>(R.id.itemFixarBotao)

        switchFixar.isEnabled = isOverlayEnabled
        switchFixar.isClickable = isOverlayEnabled
        switchFixar.alpha = if (isOverlayEnabled) 1f else 0.45f
        itemFixar.alpha = if (isOverlayEnabled) 1f else 0.45f
    }

    private fun showLogoutDialog() {
        val dialog = AlertDialog.Builder(this)
            .setTitle("Confirmar Logout")
            .setMessage("Tem certeza que deseja fazer logout?")
            .setPositiveButton("Confirmar") { _, _ ->
                UserFlowPreferences.setShowConfiguration(this, false)
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
