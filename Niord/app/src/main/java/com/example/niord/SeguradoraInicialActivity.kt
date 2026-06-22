package com.example.niord

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

class SeguradoraInicialActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Inicializa o app mostrando a tela principal de recomendações
        mostrarTelaInicial()
    }


    private fun mostrarTelaInicial() {
        setContentView(R.layout.seguradora_inicial)

        val btnRecommendations = findViewById<MaterialButton>(R.id.btnRecommendations)
        val btnAddInsurance = findViewById<MaterialButton>(R.id.btnAddInsurance)
        val btnBack = findViewById<ImageButton>(R.id.btnBack)

        // Botão: Ver Recomendações -> Abre a outra Activity de cards (Tinder)
        btnRecommendations.setOnClickListener {
            val intent = Intent(this, SeguradoraFeedActivity::class.java)
            startActivity(intent)
        }

        // Botão: Adicionar a sua seguradora -> Muda o layout nesta mesma Activity
        btnAddInsurance.setOnClickListener {
            mostrarTelaFormulario()
        }

        // Setinha de voltar da tela inicial
        btnBack.setOnClickListener {
            finish()
        }
    }


    private fun mostrarTelaFormulario() {
        // Trocamos o layout atual pelo layout do formulário
        setContentView(R.layout.adicionar_seguradora)

        // Como o layout mudou, buscamos os botões do NOVO layout
        val btnVoltar = findViewById<ImageButton>(R.id.btnVoltar)
        val btnConfirmar = findViewById<MaterialButton>(R.id.btnConfirmar)

        // Se clicar em voltar no formulário, recarregamos a tela inicial
        btnVoltar.setOnClickListener {
            mostrarTelaInicial()
        }

        // Ação do botão confirmar do formulário
        btnConfirmar.setOnClickListener {
            // Sua lógica de validação do formulário entra aqui depois
        }
    }
}