package com.example.niord

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.InputType
import android.view.MotionEvent
import android.widget.EditText
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge

class CadastroActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Substitua pelo nome exato do seu arquivo XML de cadastro (sem o .xml)
        setContentView(R.layout.cadastro)

        val editSenha = findViewById<EditText>(R.id.editSenha)
        val editConfirmarSenha = findViewById<EditText>(R.id.editConfirmarSenha)

        // Aplica a lógica de revelar senha nos dois campos
        configurarBotaoOlho(editSenha)
        configurarBotaoOlho(editConfirmarSenha)
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

