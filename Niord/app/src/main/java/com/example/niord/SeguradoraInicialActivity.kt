package com.example.niord // Lembre-se de verificar se este é o seu pacote

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

class SeguradoraInicialActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.seguradora_inicial) // Conecta com o seu XML

        // Encontra os botões na tela
        val btnRecommendations = findViewById<MaterialButton>(R.id.btnRecommendations)
        val btnBack = findViewById<ImageButton>(R.id.btnBack)

        // (Opcional) Se quiser já deixar o botão de "Adicionar a sua" mapeado
        // val btnAddInsurance = findViewById<MaterialButton>(R.id.btnAddInsurance)

        // AÇÃO: Ao clicar em "Ver recomendações"
        btnRecommendations.setOnClickListener {
            // Cria a intenção de ir para a SeguradoraFeedActivity
            val intent = Intent(this, SeguradoraFeedActivity::class.java)
            startActivity(intent)
        }

        // AÇÃO: Ao clicar na setinha de voltar no topo
        btnBack.setOnClickListener {
            finish() // Fecha esta tela e volta para a anterior
        }
    }
}