package com.example.niord

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.view.LayoutInflater
import android.view.MotionEvent
import android.widget.Button
import android.widget.EditText
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.niord.databinding.CadastroBinding
import android.util.Patterns
import android.widget.ImageView
import android.widget.TextView
import androidx.core.text.isDigitsOnly
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import com.example.niord.api.ApiClient
import com.example.niord.api.ApiService
import com.example.niord.api.ErrorResponse
import com.example.niord.api.RegisterPost
import com.example.niord.api.cpfPlainToFormatted
import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.launch

class CadastroActivity : ComponentActivity() {

    private lateinit var binding: CadastroBinding
    private lateinit var apiService: ApiService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        apiService = ApiService()

        // Substitua pelo nome exato do seu arquivo XML de cadastro (sem o .xml)
        val inflater = LayoutInflater.from(this)
        binding = CadastroBinding.inflate(inflater)
        setContentView(binding.root)
        setupSystemInsets()

        val editSenha = findViewById<EditText>(R.id.editSenha)
        val editConfirmarSenha = findViewById<EditText>(R.id.editConfirmarSenha)

        // Aplica a lógica de revelar senha nos dois campos
        configurarBotaoOlho(editSenha)
        configurarBotaoOlho(editConfirmarSenha)
        findViewById<Button>(R.id.btnCriarConta).setOnClickListener {
            if (verifyData()) {
                openOtpFlow()
            }
        }

        //Formatting bindings
        setupLiveValidation()

        findViewById<ImageView>(R.id.btnVoltar).setOnClickListener {
            finish()
        }
    }

    private fun setupSystemInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.cadastroRoot) { view, windowInsets ->
            val statusBars = windowInsets.getInsets(WindowInsetsCompat.Type.statusBars())
            val ime = windowInsets.getInsets(WindowInsetsCompat.Type.ime())
            view.setPadding(
                statusBars.left,
                statusBars.top,
                statusBars.right,
                ime.bottom
            )
            windowInsets
        }
        ViewCompat.requestApplyInsets(binding.cadastroRoot)
    }

    private fun setupLiveValidation() {
        binding.editNome.addTextChangedListener {
            validateFirstNameField()
        }
        binding.editSobrenome.addTextChangedListener {
            validateLastNameField()
        }
        binding.editEmail.addTextChangedListener {
            validateEmailField()
        }
        binding.editCpf.addTextChangedListener {
            validateCpfField()
        }
        binding.editTelefone.addTextChangedListener {
            validatePhoneField()
        }
        binding.editPlaca.addTextChangedListener {
            validatePlateField()
        }
        binding.editSenha.addTextChangedListener {
            validatePasswordField()
        }
        binding.editConfirmarSenha.addTextChangedListener {
            validatePasswordField()
        }
    }

    fun verifyData(): Boolean{
        var validData = true

        if (!validateFirstNameField(showRequiredError = true)) validData = false

        if (!validateLastNameField(showRequiredError = true)) validData = false

        if (!validateEmailField(showRequiredError = true)) validData = false
        if (!validateCpfField(showRequiredError = true)) validData = false
        if (!validatePlateField(showRequiredError = true)) validData = false
        if (!validatePhoneField(showRequiredError = true)) validData = false
        if (!validatePasswordField(showRequiredError = true)) validData = false


        return validData
    }

    fun sendData(){
        lifecycleScope.launch {
            val requestBody = RegisterPost(
                name = binding.editNome.text.toString().trim() + " " + binding.editSobrenome.text.toString().trim(),
                email = binding.editEmail.text.toString().trim(),
                password = binding.editSenha.text.toString(),
                //Remove the '-' for agreed formatting with the API
                registrationPlate = binding.editPlaca.text.toString().trim().filter {c -> c != '-'},
                cpf = cpfPlainToFormatted(binding.editCpf.text.toString().filter { c -> c.isDigit() }),
                telephone = binding.editTelefone.text.toString().trim(),
                bloodType = binding.spinnerTipoSanguineo.selectedItem.toString().ifEmpty{ null }
            )
            val response = apiService.sendRegisterData(requestBody)
            if(response.status.value == 200) {
                println(response.bodyAsText())
            }else if(response.status.value == 401){
                val errorMessage = response.body<ErrorResponse>()
                println(errorMessage.detail.message)
                println(errorMessage.detail.type)
                println(errorMessage.detail.field)
            }

        }
    }

    fun testRequest() {
        lifecycleScope.launch {
            println(apiService.greet())
        }
    }

    private fun openOtpFlow() {
        val intent = Intent(this, OtpActivity::class.java).apply {
            putExtra(OtpActivity.EXTRA_EMAIL, binding.editEmail.text.toString().trim())
        }
        startActivity(intent)
    }

    private fun validateFirstNameField(showRequiredError: Boolean = false): Boolean {
        val firstName = binding.editNome.text.toString().trim()
        if (firstName.isEmpty()) {
            binding.editNome.error = if (showRequiredError) "Nome obrigatório" else null
            return false
        }

        return when (val result = validatePersonNameDetailed(firstName, "Nome")) {
            is FieldValidationResult.Valid -> {
                binding.editNome.error = null
                true
            }
            is FieldValidationResult.Invalid -> {
                binding.editNome.error = result.message
                false
            }
        }
    }

    private fun validateLastNameField(showRequiredError: Boolean = false): Boolean {
        val lastName = binding.editSobrenome.text.toString().trim()
        if (lastName.isEmpty()) {
            binding.editSobrenome.error = if (showRequiredError) "Sobrenome obrigatório" else null
            return false
        }

        return when (val result = validatePersonNameDetailed(lastName, "Sobrenome")) {
            is FieldValidationResult.Valid -> {
                binding.editSobrenome.error = null
                true
            }
            is FieldValidationResult.Invalid -> {
                binding.editSobrenome.error = result.message
                false
            }
        }
    }

    private fun validateEmailField(showRequiredError: Boolean = false): Boolean {
        val email = binding.editEmail.text.toString().trim()
        if (email.isEmpty()) {
            binding.editEmail.error = if (showRequiredError) "Email obrigatório" else null
            return false
        }

        return when (val result = validateEmailDetailed(email)) {
            is FieldValidationResult.Valid -> {
                binding.editEmail.error = null
                true
            }
            is FieldValidationResult.Invalid -> {
                binding.editEmail.error = result.message
                false
            }
        }
    }

    private fun validateCpfField(showRequiredError: Boolean = false): Boolean {
        val cpf = binding.editCpf.text.toString().filter { c -> c.isDigit() }
        if (cpf.isEmpty()) {
            binding.editCpf.error = if (showRequiredError) "CPF obrigatório" else null
            return false
        }

        return when (val result = validateCpfDetailed(cpf)) {
            is FieldValidationResult.Valid -> {
                binding.editCpf.error = null
                true
            }
            is FieldValidationResult.Invalid -> {
                binding.editCpf.error = result.message
                false
            }
        }
    }

    private fun validatePhoneField(showRequiredError: Boolean = false): Boolean {
        val phone = binding.editTelefone.text.toString().trim()
        if (phone.isEmpty()) {
            binding.editTelefone.error = if (showRequiredError) "Telefone obrigatório" else null
            return false
        }

        return when (val result = validatePhoneDetailed(phone)) {
            is FieldValidationResult.Valid -> {
                binding.editTelefone.error = null
                true
            }
            is FieldValidationResult.Invalid -> {
                binding.editTelefone.error = result.message
                false
            }
        }
    }

    private fun validatePlateField(showRequiredError: Boolean = false): Boolean {
        val plate = binding.editPlaca.text.toString().trim().uppercase()
        if (plate.isEmpty()) {
            binding.editPlaca.error = if (showRequiredError) "Placa obrigatória" else null
            return false
        }

        return when (val result = validatePlateDetailed(plate)) {
            is FieldValidationResult.Valid -> {
                binding.editPlaca.error = null
                true
            }
            is FieldValidationResult.Invalid -> {
                binding.editPlaca.error = result.message
                false
            }
        }
    }

    fun passwordValidationStep(): Boolean{
        var valid = true
        val password = binding.editSenha.text.toString()
        val passwordConfirm = binding.editConfirmarSenha.text.toString()
        val isMatchingPassword = password == passwordConfirm
        if(!isMatchingPassword){
            println("Ué")
            binding.editConfirmarSenha.error = "As senhas não são iguais "
            valid = false
        }else{
            binding.editConfirmarSenha.error = null
        }
        val validationResult = validatePassword(password)
        if(validationResult is PasswordResult.Invalid){
            val errors = validationResult.errors.joinToString("\n")
            binding.editSenha.error = errors
            valid = false
        }
        return valid
    }

    private fun validatePasswordField(showRequiredError: Boolean = false): Boolean {
        if (binding.editSenha.text.isNullOrEmpty() && binding.editConfirmarSenha.text.isNullOrEmpty()) {
            binding.editSenha.error = if (showRequiredError) "Senha obrigatória" else null
            binding.editConfirmarSenha.error = if (showRequiredError) "Confirmação obrigatória" else null
            return false
        }

        return passwordValidationStep()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun configurarBotaoOlho(editText: EditText) {
        var isSenhaVisivel = false

        editText.setOnTouchListener { _, event ->
            val DRAWABLE_RIGHT = 2

            if (event.action == MotionEvent.ACTION_UP) {
                if (event.rawX >= (editText.right - editText.compoundDrawables[DRAWABLE_RIGHT].bounds.width() - editText.paddingRight)) {

                    isSenhaVisivel = !isSenhaVisivel

                    if (isSenhaVisivel) {
                        editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                    } else {
                        editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                        editText.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_visibility_off, 0)
                    }

                    editText.setSelection(editText.text.length)
                    return@setOnTouchListener true
                }
            }
            false
        }
    }
}
