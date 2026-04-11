package com.example.niord

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.InputType
import android.view.MotionEvent
import android.widget.EditText
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge

class LoginActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Substitua pelo nome exato do seu arquivo XML de login (ex: R.layout.activity_login)
        setContentView(R.layout.login)

        // Puxa o campo de senha do XML da tela de login
        val editLoginSenha = findViewById<EditText>(R.id.editLoginSenha)

        // Aplica a lógica de revelar/esconder a senha
        configurarBotaoOlho(editLoginSenha)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun configurarBotaoOlho(editText: EditText) {
        var isSenhaVisivel = false

        editText.setOnTouchListener { _, event ->
            val DRAWABLE_RIGHT = 2

            if (event.action == MotionEvent.ACTION_UP) {
                // Checa se o toque foi em cima do ícone (no lado direito)
                if (event.rawX >= (editText.right - editText.compoundDrawables[DRAWABLE_RIGHT].bounds.width() - editText.paddingRight)) {

                    isSenhaVisivel = !isSenhaVisivel

                    if (isSenhaVisivel) {
                        // MOSTRA A SENHA
                        editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                        // Se tiver um ícone de olho aberto, descomente a linha abaixo e coloque o nome dele:
                        // editText.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_visibility, 0)
                    } else {
                        // ESCONDE A SENHA (Fica em pontinhos)
                        editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                        editText.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_visibility_off, 0)
                    }

                    // Mantém o cursor piscando no final da palavra digitada
                    editText.setSelection(editText.text.length)

                    return@setOnTouchListener true
                }
            }
            false
        }
    }
}