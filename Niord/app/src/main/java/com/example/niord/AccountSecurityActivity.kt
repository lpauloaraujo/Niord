package com.example.niord

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge

class AccountSecurityActivity : ComponentActivity() {
    private val prefs by lazy { getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }
    private var lockTimer: CountDownTimer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.account_security)

        findViewById<ImageView>(R.id.btnVoltar).setOnClickListener { finish() }
        findViewById<Button>(R.id.btnCancelar).setOnClickListener { finish() }
        findViewById<Button>(R.id.btnRetornar).setOnClickListener { finish() }
        findViewById<Button>(R.id.btnConfirmarSenha).setOnClickListener { validatePasswordGate() }

        renderState()
    }

    override fun onDestroy() {
        lockTimer?.cancel()
        super.onDestroy()
    }

    private fun renderState() {
        val remaining = getRemainingLockMillis()
        if (remaining > 0) {
            showBlockedState(remaining)
        } else {
            prefs.edit()
                .putInt(KEY_ATTEMPTS, 0)
                .putLong(KEY_LOCK_UNTIL, 0L)
                .apply()
            showPasswordState()
        }
    }

    private fun validatePasswordGate() {
        val passwordInput = findViewById<EditText>(R.id.editSenhaAtual)
        val errorText = findViewById<TextView>(R.id.textErroSenha)
        val password = passwordInput.text.toString()

        if (password == CURRENT_PASSWORD) {
            prefs.edit()
                .putInt(KEY_ATTEMPTS, 0)
                .putLong(KEY_LOCK_UNTIL, 0L)
                .apply()
            startActivity(Intent(this, AccountDataActivity::class.java))
            finish()
            return
        }

        val attempts = prefs.getInt(KEY_ATTEMPTS, 0) + 1
        if (attempts >= MAX_ATTEMPTS) {
            prefs.edit()
                .putInt(KEY_ATTEMPTS, attempts)
                .putLong(KEY_LOCK_UNTIL, System.currentTimeMillis() + LOCK_DURATION_MILLIS)
                .apply()
            showBlockedState(LOCK_DURATION_MILLIS)
            return
        }

        prefs.edit().putInt(KEY_ATTEMPTS, attempts).apply()
        errorText.text = "Senha incorreta. Tentativas restantes: ${MAX_ATTEMPTS - attempts}"
        errorText.visibility = View.VISIBLE
        passwordInput.text.clear()
    }

    private fun showPasswordState() {
        lockTimer?.cancel()
        findViewById<LinearLayout>(R.id.panelPassword).visibility = View.VISIBLE
        findViewById<LinearLayout>(R.id.panelBlocked).visibility = View.GONE
    }

    private fun showBlockedState(remainingMillis: Long) {
        findViewById<LinearLayout>(R.id.panelPassword).visibility = View.GONE
        findViewById<LinearLayout>(R.id.panelBlocked).visibility = View.VISIBLE
        startLockCountdown(remainingMillis)
    }

    private fun startLockCountdown(remainingMillis: Long) {
        lockTimer?.cancel()
        val countdownText = findViewById<TextView>(R.id.textCountdown)
        lockTimer = object : CountDownTimer(remainingMillis, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                countdownText.text =
                    "Faltam ${formatMillis(millisUntilFinished)} para você ter acesso às informações da conta"
            }

            override fun onFinish() {
                renderState()
            }
        }.start()
    }

    private fun getRemainingLockMillis(): Long {
        val lockUntil = prefs.getLong(KEY_LOCK_UNTIL, 0L)
        return (lockUntil - System.currentTimeMillis()).coerceAtLeast(0L)
    }

    private fun formatMillis(millis: Long): String {
        val totalSeconds = ((millis + 999L) / 1000L).toInt()
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return "%02d:%02d".format(minutes, seconds)
    }

    companion object {
        const val CURRENT_PASSWORD = "12345678"
        private const val PREFS_NAME = "account_security_flow"
        private const val KEY_ATTEMPTS = "attempts"
        private const val KEY_LOCK_UNTIL = "lock_until"
        private const val MAX_ATTEMPTS = 3
        private const val LOCK_DURATION_MILLIS = 3 * 60 * 1000L
    }
}
