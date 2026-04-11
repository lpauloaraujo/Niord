package com.example.niord

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.MotionEvent
import android.widget.Button
import android.widget.EditText
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import com.example.niord.databinding.CadastroBinding
import android.util.Patterns
import androidx.core.text.isDigitsOnly

class CadastroActivity : ComponentActivity() {

    private lateinit var binding: CadastroBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

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
        }
    }

    fun verifyData(): Boolean{
        var validData = true

        //val name = binding.editNome.text.toString() + " " + binding.editSobrenome.text.toString()
        val email = binding.editEmail.text.toString()

        val isEmailValid: Boolean = validateEmail(email)
        if(!isEmailValid) {binding.editEmail.error = "Email inválido"; validData = false}

        val cpf = binding.editCpf.text.toString()
        val isValidCpf = validateCpf(cpf)
        if(!isValidCpf){binding.editCpf.error = "CPF inválido"; validData = false}

        val plate = binding.editPlaca.text.toString().uppercase()
        val isValidPlate = validatePlate(plate)
        if(!isValidPlate){binding.editPlaca.error = "Placa inválida"; validData = false}

        val phone = binding.editTelefone.text.toString()
        val isValidPhone = validatePone(phone)
        if(!isValidPhone){binding.editTelefone.error = "Telefone Inválido"; validData = false}

        return validData
    }

    fun sendData(){

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

