package com.example.niord

import android.app.AlertDialog
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.Window
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.example.niord.api.ApiService
import com.example.niord.api.ErrorResponse
import com.example.niord.api.User
import com.example.niord.api.UserUpdatePatch
import io.ktor.client.call.body
import kotlinx.coroutines.launch

class AccountDataActivity : ComponentActivity() {
    private val otpTimerHandler = Handler(Looper.getMainLooper())
    private var otpCooldownEndsAt = 0L
    private val otpTimerRunnable = object : Runnable {
        override fun run() {
            updateOtpTimer()
        }
    }
    private lateinit var apiService: ApiService

    private lateinit var editNome: EditText
    private lateinit var editSobrenome: EditText
    private lateinit var editCpf: EditText
    private lateinit var editEmail: EditText
    private lateinit var editOtpEmail: EditText
    private lateinit var editTelefone: EditText
    private lateinit var editSenhaTelefone: EditText
    private lateinit var editPlaca: EditText
    private lateinit var editNovaSenha: EditText
    private lateinit var editSenhaAtualNova: EditText
    private lateinit var spinnerTipoSanguineo: Spinner
    private lateinit var panelOtpEmail: LinearLayout
    private lateinit var panelSenhaTelefone: LinearLayout
    private lateinit var panelSenhaAtual: LinearLayout
    private lateinit var errorText: TextView
    private lateinit var resendButton: Button
    private lateinit var emailStatusText: TextView
    private lateinit var phoneStatusText: TextView

    private var originalEmail = "j.almeida@outlook.com"
    private var originalPhone = "(81) 98888-8888"
    private var phoneConfirmed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.account_data)

        apiService = ApiService(this)
        bindViews()
        setupActions()
        loadUserData()
    }

    override fun onDestroy() {
        otpTimerHandler.removeCallbacks(otpTimerRunnable)
        super.onDestroy()
    }

    private fun bindViews() {
        editNome = findViewById(R.id.editNome)
        editSobrenome = findViewById(R.id.editSobrenome)
        editCpf = findViewById(R.id.editCpf)
        editEmail = findViewById(R.id.editEmail)
        editOtpEmail = findViewById(R.id.editOtpEmail)
        editTelefone = findViewById(R.id.editTelefone)
        editSenhaTelefone = findViewById(R.id.editSenhaTelefone)
        editPlaca = findViewById(R.id.editPlaca)
        editNovaSenha = findViewById(R.id.editNovaSenha)
        editSenhaAtualNova = findViewById(R.id.editSenhaAtualNova)
        spinnerTipoSanguineo = findViewById(R.id.spinnerTipoSanguineo)
        panelOtpEmail = findViewById(R.id.panelOtpEmail)
        panelSenhaTelefone = findViewById(R.id.panelSenhaTelefone)
        panelSenhaAtual = findViewById(R.id.panelSenhaAtual)
        errorText = findViewById(R.id.textErroFormulario)
        resendButton = findViewById(R.id.btnReenviarOtp)
        resendButton.text = "Enviar código"
        resendButton.isEnabled = true
        emailStatusText = findViewById(R.id.textStatusEmail)
        phoneStatusText = findViewById(R.id.textStatusTelefone)
    }

    private fun loadUserData() {
        if (DebugPreferences.isDebug(this)) {
            applyUserData(
                User(
                    id = 0,
                    registration_plate = "MWR1160",
                    email = originalEmail,
                    telephone = originalPhone,
                    is_verified = true,
                    name = "José Almeida",
                    cpf = "123.456.789-09",
                    blood_type = "A+"
                )
            )
            return
        }

        lifecycleScope.launch {
            try {
                val response = apiService.getUser()
                if (response.status.value == 200) {
                    applyUserData(response.body())
                } else {
                    showBackendError(response.body())
                }
            } catch (e: Exception) {
                showGeneralError("Não foi possível carregar seus dados")
            }
        }
    }

    private fun applyUserData(user: User) {
        val nameParts = user.name.trim().split(Regex("\\s+"), limit = 2)
        editNome.setText(nameParts.getOrNull(0).orEmpty())
        editSobrenome.setText(nameParts.getOrNull(1).orEmpty())
        editCpf.setText(user.cpf)
        editEmail.setText(user.email)
        editTelefone.setText(user.telephone)
        editPlaca.setText(user.registration_plate)
        setBloodTypeSelection(user.blood_type)

        originalEmail = user.email
        originalPhone = user.telephone
        phoneConfirmed = false
    }

    private fun setBloodTypeSelection(bloodType: String?) {
        if (bloodType.isNullOrBlank()) return

        for (index in 0 until spinnerTipoSanguineo.count) {
            if (spinnerTipoSanguineo.getItemAtPosition(index).toString() == bloodType) {
                spinnerTipoSanguineo.setSelection(index)
                return
            }
        }
    }

    private fun setupActions() {
        findViewById<ImageView>(R.id.btnVoltar).setOnClickListener { finish() }
        findViewById<Button>(R.id.btnCancelar).setOnClickListener { finish() }
        findViewById<Button>(R.id.btnSalvar).setOnClickListener { saveChanges() }
        findViewById<Button>(R.id.btnConfirmarOtp).setOnClickListener { confirmEmailOtp() }
        findViewById<Button>(R.id.btnConfirmarTelefone).setOnClickListener { confirmPhonePassword() }

        findViewById<ImageButton>(R.id.btnEditCpf).setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("CPF bloqueado")
                .setMessage("Para alterar o CPF, entre em contato com o suporte do Niord.")
                .setPositiveButton("Entendi", null)
                .show()
        }

        listOf(
            R.id.btnEditNome to editNome,
            R.id.btnEditSobrenome to editSobrenome,
            R.id.btnEditEmail to editEmail,
            R.id.btnEditTelefone to editTelefone,
            R.id.btnEditPlaca to editPlaca
        ).forEach { (buttonId, field) ->
            findViewById<ImageButton>(buttonId).setOnClickListener {
                field.requestFocus()
                field.setSelection(field.text.length)
            }
        }

        findViewById<ImageButton>(R.id.btnEditTipoSanguineo).setOnClickListener {
            spinnerTipoSanguineo.performClick()
        }

        findViewById<ImageButton>(R.id.btnEditSenha).setOnClickListener {
            panelSenhaAtual.visibility = View.VISIBLE
            editNovaSenha.isEnabled = true
            editSenhaAtualNova.requestFocus()
        }

        findViewById<ImageButton>(R.id.btnEditEmail).setOnClickListener {
            editEmail.requestFocus()
            panelOtpEmail.visibility = View.VISIBLE
            requestEmailOtp()
        }

        findViewById<ImageButton>(R.id.btnEditTelefone).setOnClickListener {
            editTelefone.requestFocus()
            panelSenhaTelefone.visibility = View.VISIBLE
        }

        resendButton.setOnClickListener {
            requestEmailOtp()
        }

        setupFieldValidation()
    }

    private fun setupFieldValidation() {
        editNome.addTextChangedListener(simpleWatcher {
            validateNameField()
            hideGeneralError()
        })

        editSobrenome.addTextChangedListener(simpleWatcher {
            validateLastNameField()
            hideGeneralError()
        })

        editEmail.addTextChangedListener(simpleWatcher {
            resetEmailOtpState()
            emailStatusText.visibility = View.GONE
            validateEmailField()
            hideGeneralError()
        })

        editTelefone.addTextChangedListener(simpleWatcher {
            phoneConfirmed = false
            phoneStatusText.visibility = View.GONE
            validatePhoneField()
            hideGeneralError()
        })

        editPlaca.addTextChangedListener(simpleWatcher {
            validatePlateField()
            hideGeneralError()
        })

        editNovaSenha.addTextChangedListener(simpleWatcher {
            validateNewPasswordField()
            hideGeneralError()
        })
    }

    private fun requestEmailOtp() {
        val email = editEmail.text.toString().trim()
        if (!validateEmailField(showRequiredError = true)) {
            return
        }

        if (email == originalEmail) {
            Toast.makeText(this, "Informe um novo email para enviar o código", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                val response = apiService.requestAccountEmailOtp(email)
                if (response.status.value == 200) {
                    startOtpTimer()
                    Toast.makeText(this@AccountDataActivity, "Código OTP enviado para o novo email", Toast.LENGTH_SHORT).show()
                } else {
                    showBackendError(response.body())
                }
            } catch (e: Exception) {
                showGeneralError("Não foi possível enviar o código OTP")
            }
        }
    }

    private fun saveChanges() {
        clearErrors()

        if (!validateBasicFields(showRequiredError = true)) {
            showInvalidDataError()
            return
        }

        if (emailChanged() && !validateEmailSecurity()) {
            return
        }

        if (phoneChanged() && !validatePhoneSecurity()) {
            return
        }

        if (passwordChanged() && !validatePasswordSecurity()) {
            return
        }

        submitChanges()
    }

    private fun submitChanges() {
        val currentPassword = currentPasswordForSensitiveChanges()
        val emailOtpCode = if (emailChanged()) {
            editOtpEmail.text.toString().toIntOrNull()
        } else {
            null
        }

        val payload = UserUpdatePatch(
            name = "${editNome.text.toString().trim()} ${editSobrenome.text.toString().trim()}".trim(),
            email = editEmail.text.toString().trim(),
            telephone = editTelefone.text.toString().trim(),
            registrationPlate = normalizePlate(editPlaca.text.toString()),
            bloodType = spinnerTipoSanguineo.selectedItem?.toString()?.takeIf { it.isNotBlank() },
            newPassword = editNovaSenha.text.toString().takeIf { it.isNotBlank() },
            currentPassword = currentPassword,
            emailOtpCode = emailOtpCode
        )

        lifecycleScope.launch {
            try {
                val response = apiService.updateUser(payload)
                if (response.status.value == 200) {
                    applyUserData(response.body())
                    showSuccessDialog()
                } else {
                    showBackendError(response.body())
                }
            } catch (e: Exception) {
                showGeneralError("Não foi possível salvar as alterações")
            }
        }
    }

    private fun currentPasswordForSensitiveChanges(): String? {
        val passwordForNewPassword = editSenhaAtualNova.text.toString()
        val passwordForPhone = editSenhaTelefone.text.toString()

        return when {
            passwordChanged() && passwordForNewPassword.isNotBlank() -> passwordForNewPassword
            phoneChanged() && passwordForPhone.isNotBlank() -> passwordForPhone
            passwordForNewPassword.isNotBlank() -> passwordForNewPassword
            passwordForPhone.isNotBlank() -> passwordForPhone
            else -> null
        }
    }

    private fun validateBasicFields(showRequiredError: Boolean = false): Boolean {
        var valid = true

        valid = validateNameField(showRequiredError) && valid
        valid = validateLastNameField(showRequiredError) && valid
        valid = validateEmailField(showRequiredError) && valid
        valid = validatePhoneField(showRequiredError) && valid
        valid = validatePlateField(showRequiredError) && valid
        valid = validateNewPasswordField() && valid

        return valid
    }

    private fun validateEmailSecurity(): Boolean {
        panelOtpEmail.visibility = View.VISIBLE
        val code = editOtpEmail.text.toString()
        if (code.length != OTP_CODE_LENGTH || code.toIntOrNull() == null) {
            emailStatusText.visibility = View.GONE
            editOtpEmail.error = "Informe o código de 6 dígitos"
            return false
        }
        return true
    }

    private fun validatePhoneSecurity(): Boolean {
        panelSenhaTelefone.visibility = View.VISIBLE
        if (!phoneConfirmed) {
            editSenhaTelefone.error = "Confirme a senha antes de salvar"
            return false
        }
        return true
    }

    private fun validatePasswordSecurity(): Boolean {
        panelSenhaAtual.visibility = View.VISIBLE
        var valid = true

        if (editSenhaAtualNova.text.toString().isBlank()) {
            editSenhaAtualNova.error = "Senha atual obrigatória"
            valid = false
        }

        valid = when (val passwordResult = validatePassword(editNovaSenha.text.toString())) {
            PasswordResult.Valid -> valid
            is PasswordResult.Invalid -> {
                editNovaSenha.error = passwordResult.errors.joinToString("\n")
                false
            }
        }

        return valid
    }

    private fun clearErrors() {
        errorText.visibility = View.GONE
        listOf(
            editNome,
            editSobrenome,
            editEmail,
            editTelefone,
            editPlaca,
            editOtpEmail,
            editSenhaTelefone,
            editNovaSenha,
            editSenhaAtualNova
        ).forEach { it.error = null }
    }

    private fun showInvalidDataError() {
        errorText.text = "Dados inválidos: verifique os caracteres"
        errorText.visibility = View.VISIBLE
    }

    private fun showBackendError(errorResponse: ErrorResponse) {
        val message = errorResponse.detail.message
        when (errorResponse.detail.field) {
            "name" -> editNome.error = message
            "email" -> editEmail.error = message
            "email_otp_code" -> editOtpEmail.error = message
            "telephone" -> editTelefone.error = message
            "registration_plate" -> editPlaca.error = message
            "new_password" -> editNovaSenha.error = message
            "current_password" -> {
                editSenhaTelefone.error = message
                editSenhaAtualNova.error = message
            }
            else -> showGeneralError(message)
        }

        if (errorResponse.detail.field != null) {
            showGeneralError(message)
        }
    }

    private fun showGeneralError(message: String) {
        errorText.text = message
        errorText.visibility = View.VISIBLE
    }

    private fun hideGeneralError() {
        errorText.visibility = View.GONE
    }

    private fun validateNameField(showRequiredError: Boolean = false): Boolean {
        return validateRequiredDetailedField(
            field = editNome,
            label = "Nome",
            showRequiredError = showRequiredError,
            validation = { validatePersonNameDetailed(it, "Nome") }
        )
    }

    private fun validateLastNameField(showRequiredError: Boolean = false): Boolean {
        return validateRequiredDetailedField(
            field = editSobrenome,
            label = "Sobrenome",
            showRequiredError = showRequiredError,
            validation = { validatePersonNameDetailed(it, "Sobrenome") }
        )
    }

    private fun validateEmailField(showRequiredError: Boolean = false): Boolean {
        return validateRequiredDetailedField(
            field = editEmail,
            label = "Email",
            showRequiredError = showRequiredError,
            validation = { validateEmailDetailed(it) }
        )
    }

    private fun validatePhoneField(showRequiredError: Boolean = false): Boolean {
        return validateRequiredDetailedField(
            field = editTelefone,
            label = "Telefone",
            showRequiredError = showRequiredError,
            validation = { validatePhoneDetailed(it) }
        )
    }

    private fun validatePlateField(showRequiredError: Boolean = false): Boolean {
        return validateRequiredDetailedField(
            field = editPlaca,
            label = "Placa",
            showRequiredError = showRequiredError,
            validation = { validatePlateDetailed(it) }
        )
    }

    private fun validateNewPasswordField(): Boolean {
        val password = editNovaSenha.text.toString()
        if (password.isBlank()) {
            editNovaSenha.error = null
            return true
        }

        if (!validateMaxLength(editNovaSenha, "Senha")) {
            return false
        }

        return when (val passwordResult = validatePassword(password)) {
            PasswordResult.Valid -> {
                editNovaSenha.error = null
                true
            }
            is PasswordResult.Invalid -> {
                editNovaSenha.error = passwordResult.errors.joinToString("\n")
                false
            }
        }
    }

    private fun validateRequiredDetailedField(
        field: EditText,
        label: String,
        showRequiredError: Boolean,
        validation: (String) -> FieldValidationResult
    ): Boolean {
        val value = field.text.toString().trim()
        if (value.isEmpty()) {
            field.error = if (showRequiredError) "$label obrigatório" else null
            return false
        }

        if (!validateMaxLength(field, label)) {
            return false
        }

        return validateDetailedField(field, validation(value))
    }

    private fun validateDetailedField(
        field: EditText,
        result: FieldValidationResult
    ): Boolean {
        return when (result) {
            FieldValidationResult.Valid -> {
                field.error = null
                true
            }
            is FieldValidationResult.Invalid -> {
                field.error = result.message
                false
            }
        }
    }

    private fun validateMaxLength(field: EditText, label: String): Boolean {
        if (field.text.length <= 60) {
            return true
        }

        field.error = "$label deve ter no máximo 60 caracteres"
        return false
    }

    private fun confirmEmailOtp() {
        editOtpEmail.error = null
        val code = editOtpEmail.text.toString()
        if (code.length != OTP_CODE_LENGTH || code.toIntOrNull() == null) {
            emailStatusText.visibility = View.GONE
            editOtpEmail.error = "Código inválido"
            return
        }

        emailStatusText.text = "Código informado. Ele será validado ao salvar."
        emailStatusText.visibility = View.VISIBLE
        Toast.makeText(this, "Código informado", Toast.LENGTH_SHORT).show()
    }

    private fun confirmPhonePassword() {
        editSenhaTelefone.error = null
        if (editSenhaTelefone.text.toString().isBlank()) {
            phoneConfirmed = false
            phoneStatusText.visibility = View.GONE
            editSenhaTelefone.error = "Senha atual obrigatória"
            return
        }

        phoneConfirmed = true
        phoneStatusText.text = "Senha informada. Salve para concluir a alteração."
        phoneStatusText.visibility = View.VISIBLE
        Toast.makeText(this, "Senha informada", Toast.LENGTH_SHORT).show()
    }

    private fun emailChanged(): Boolean = editEmail.text.toString().trim() != originalEmail

    private fun phoneChanged(): Boolean = editTelefone.text.toString().trim() != originalPhone

    private fun passwordChanged(): Boolean = editNovaSenha.text.toString().isNotBlank()

    private fun startOtpTimer() {
        otpTimerHandler.removeCallbacks(otpTimerRunnable)
        otpCooldownEndsAt = System.currentTimeMillis() + OTP_RESEND_COOLDOWN_MILLIS
        resendButton.isEnabled = false
        updateOtpTimer()
    }

    private fun updateOtpTimer() {
        val remainingMillis = otpCooldownEndsAt - System.currentTimeMillis()
        if (remainingMillis <= 0L) {
            otpTimerHandler.removeCallbacks(otpTimerRunnable)
            resendButton.isEnabled = true
            resendButton.text = "Reenviar código"
            return
        }

        val seconds = ((remainingMillis + 999L) / 1000L).toInt()
        resendButton.text = "Reenviar código em ${seconds}s"
        otpTimerHandler.postDelayed(otpTimerRunnable, OTP_TIMER_TICK_MILLIS)
    }

    private fun resetEmailOtpState() {
        otpTimerHandler.removeCallbacks(otpTimerRunnable)
        otpCooldownEndsAt = 0L
        editOtpEmail.text.clear()
        resendButton.isEnabled = true
        resendButton.text = "Enviar código"
    }

    private fun showSuccessDialog() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_account_update_success)
        dialog.setCancelable(false)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.show()

        findViewById<View>(android.R.id.content).postDelayed({
            dialog.dismiss()
            finish()
        }, SUCCESS_DIALOG_DURATION_MILLIS)
    }

    private fun simpleWatcher(onChanged: () -> Unit): TextWatcher {
        return object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                onChanged()
            }

            override fun afterTextChanged(s: Editable?) = Unit
        }
    }

    companion object {
        private const val OTP_CODE_LENGTH = 6
        private const val OTP_RESEND_COOLDOWN_MILLIS = 60_000L
        private const val OTP_TIMER_TICK_MILLIS = 250L
        private const val SUCCESS_DIALOG_DURATION_MILLIS = 1800L
    }
}
