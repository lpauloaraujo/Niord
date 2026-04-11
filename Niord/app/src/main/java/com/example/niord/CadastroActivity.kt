package com.example.niord

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.view.LayoutInflater
import android.view.MotionEvent
import android.widget.Button
import android.widget.EditText
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import com.example.niord.databinding.CadastroBinding
import android.util.Patterns
import android.widget.TextView
import androidx.core.text.isDigitsOnly
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import com.example.niord.api.ApiClient
import com.example.niord.api.ApiService
import com.example.niord.api.GreetString
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

        val editSenha = findViewById<EditText>(R.id.editSenha)
        val editConfirmarSenha = findViewById<EditText>(R.id.editConfirmarSenha)

        // Aplica a lógica de revelar senha nos dois campos
        configurarBotaoOlho(editSenha)
        configurarBotaoOlho(editConfirmarSenha)
        findViewById<Button>(R.id.btnCriarConta).setOnClickListener {
            if(verifyData()) sendData()
            testRequest()
        }

        //Formatting bindings
        binding.editSenha.addTextChangedListener {
            if(binding.editSenha.text.isNotEmpty()) passwordValidationStep()
        }
        binding.editConfirmarSenha.addTextChangedListener {
            if(binding.editConfirmarSenha.text.isNotEmpty()) passwordValidationStep()
        }

    }

    fun verifyData(): Boolean{
        var validData = true

        //val name = binding.editNome.text.toString() + " " + binding.editSobrenome.text.toString()
        val email = binding.editEmail.text.toString()

        val isEmailValid: Boolean = validateEmail(email)
        if(!isEmailValid) {binding.editEmail.error = "Email inválido"; validData = false}

        val cpf = binding.editCpf.text.toString() + "a"
        val isValidCpf = validateCpf(cpf.filter {c -> c.isDigit()})
        if(!isValidCpf){binding.editCpf.error = "CPF inválido"; validData = false}

        val plate = binding.editPlaca.text.toString().uppercase()
        val isValidPlate = validatePlate(plate)
        if(!isValidPlate){binding.editPlaca.error = "Placa inválida"; validData = false}

        val phone = binding.editTelefone.text.toString()
        val isValidPhone = validatePone(phone)
        if(!isValidPhone){binding.editTelefone.error = "Telefone Inválido"; validData = false}

        val isValidPassword = passwordValidationStep()
        if(!isValidPassword){validData = false}


        return validData
    }

    fun sendData(){
    }

    fun testRequest() {
        lifecycleScope.launch {
            println(apiService.greet())

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

