package com.example.niord

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.CountDownTimer
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.Window
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.niord.api.ApiService
import com.example.niord.api.OtpResend
import com.example.niord.api.OtpVerify
import com.example.niord.databinding.ActivityOtpBinding
import com.example.niord.databinding.DialogOtpSuccessBinding
import kotlinx.coroutines.launch

class OtpActivity : ComponentActivity() {

    private lateinit var binding: ActivityOtpBinding
    private var resendTimer: CountDownTimer? = null

    private lateinit var apiService: ApiService


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityOtpBinding.inflate(LayoutInflater.from(this))
        setContentView(binding.root)
        binding.otpRoot.applyStatusBarPadding()

        apiService = ApiService(this)

        setupOtpCopy()
        setupCodeField()
        setupActions()
        startResendTimer()
    }

    override fun onDestroy() {
        resendTimer?.cancel()
        super.onDestroy()
    }

    private fun setupOtpCopy() {
        val email = intent.getStringExtra(EXTRA_EMAIL).orEmpty()
        binding.txtOtpInstructions.text = if (email.isNotBlank()) {
            "Enviamos um codigo de 6 digitos para $email. Digite esse codigo no campo abaixo e toque em confirmar."
        } else {
            "Enviamos um codigo de 6 digitos por e-mail. Digite esse codigo no campo abaixo e toque em confirmar."
        }
    }

    private fun setupCodeField() {
        binding.editCodigoConfirmacao.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit

            override fun afterTextChanged(s: Editable?) {
                val code = s?.toString().orEmpty()
                binding.btnConfirmar.isEnabled = code.length == REQUIRED_CODE.length
                //binding.btnConfirmar.alpha = if (code == REQUIRED_CODE) 1f else 0.55f

                binding.editCodigoConfirmacao.error = when {
                    code.length == REQUIRED_CODE.length -> null
                    else -> "Codigo invalido"
                }
            }
        })
    }

    private fun setupActions() {
        binding.btnVoltarOtp.setOnClickListener {
            finish()
        }

        binding.btnConfirmar.setOnClickListener {
            if(DebugPreferences.isDebug(this)) {
                if (binding.editCodigoConfirmacao.text.toString() == REQUIRED_CODE) {
                    showSuccessDialog()
                }
            }else{
                lifecycleScope.launch {
                    try {
                        val userEmail: String = intent.getStringExtra(EXTRA_EMAIL)!!
                        val response = apiService.verifyOtp(
                            OtpVerify(
                                email =  userEmail,
                                code = binding.editCodigoConfirmacao.text.toString().toInt()
                            )
                        )
                        if(response.status.value == 200){
                            showSuccessDialog()
                        }else{
                            binding.editCodigoConfirmacao.error = "Código Inválido"
                        }
                    } catch (e: Exception) {
                        println("Something went wrong")
                        println(e.toString())
                    }
                }
            }
        }

        binding.btnReenviarCodigo.setOnClickListener {
            var succeed = false
            lifecycleScope.launch {
                try {
                    val response = apiService.resendOtp(OtpResend(email = intent.getStringExtra(EXTRA_EMAIL)!!))
                    succeed = response.status.value == 200
                } catch (e: Exception) {

                }
            }
            if(succeed) {
                Toast.makeText(this, "Codigo reenviado por e-mail", Toast.LENGTH_SHORT).show()
                startResendTimer()
            }
        }
    }

    private fun startResendTimer() {
        resendTimer?.cancel()
        binding.btnReenviarCodigo.isEnabled = false
        binding.btnReenviarCodigo.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray))

        resendTimer = object : CountDownTimer(RESEND_SECONDS * 1000L, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsLeft = millisUntilFinished / 1000L
                binding.txtOtpTimer.text = "Reenviar codigo em ${secondsLeft}s"
            }

            override fun onFinish() {
                binding.txtOtpTimer.text = "Voce pode solicitar um novo codigo."
                binding.btnReenviarCodigo.isEnabled = true
                binding.btnReenviarCodigo.setTextColor(Color.parseColor("#316DF6"))
            }
        }.start()
    }

    private fun showSuccessDialog() {
        val dialogBinding = DialogOtpSuccessBinding.inflate(LayoutInflater.from(this))
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(dialogBinding.root)
        dialog.setCancelable(false)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.show()
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        dialogBinding.root.postDelayed({
            dialog.dismiss()
            //openConfigurationFlow()
            finish()
        }, SUCCESS_DELAY_MILLIS)
    }

    private fun openConfigurationFlow() {
        UserFlowPreferences.setShowConfiguration(this, true)
        UserFlowPreferences.setOnboardingAvailable(this, true)
        UserFlowPreferences.setOverlayEnabled(this, false)
        UserFlowPreferences.setOverlayLocked(this, false)
        val nextActivity = if (UserFlowPreferences.shouldShowOnboarding(this)) {
            OnboardingActivity::class.java
        } else {
            ConfiguracaoActivity::class.java
        }
        val intent = Intent(this, nextActivity).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        startActivity(intent)
        finish()
    }

    companion object {
        const val EXTRA_EMAIL = "extra_email"
        private const val REQUIRED_CODE = "123456"
        private const val RESEND_SECONDS = 60
        private const val SUCCESS_DELAY_MILLIS = 2200L
    }
}
