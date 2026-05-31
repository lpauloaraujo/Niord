package com.example.niord // Troque para o pacote do seu app

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton

class SeguradoraFeedActivity : AppCompatActivity() {

    private lateinit var cardContainer: FrameLayout
    private var screenWidth = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_seguradora_feed)

        cardContainer = findViewById(R.id.cardContainer)
        screenWidth = resources.displayMetrics.widthPixels.toFloat()

        val btnRejeitar = findViewById<FloatingActionButton>(R.id.btnRejeitar)
        val btnAprovar = findViewById<FloatingActionButton>(R.id.btnAprovar)

        carregarSeguradoras()

        // Lógica para clicar no botão "X"
        btnRejeitar.setOnClickListener {
            animarCartaoDoTopo(direction = -1) // -1 = Esquerda
        }

        // Lógica para clicar no botão "Check"
        btnAprovar.setOnClickListener {
            animarCartaoDoTopo(direction = 1) // 1 = Direita
        }
    }

    private fun carregarSeguradoras() {
        // Lista fictícia. Substitua pelos seus dados reais.
        // CUIDADO: Garanta que os R.drawable existam, ou o app vai fechar.
        val lista = listOf(
            Seguradora(1, "Forti-Mora", "Voltada para motociclistas...", R.drawable.ic_launcher_foreground), // Troque pelas suas logos
            Seguradora(2, "Gran-Vox", "Pensada para entregadores...", R.drawable.ic_launcher_foreground),
            Seguradora(3, "Segura-Lastit", "Especializada em carros...", R.drawable.ic_launcher_foreground)
        )

        // Adicionamos de trás para frente para que a primeira seguradora fique no TOPO da pilha
        for (seguradora in lista.reversed()) {
            val cardView = LayoutInflater.from(this).inflate(R.layout.item_seguradora_card, cardContainer, false)

            // Preenche os dados
            cardView.findViewById<TextView>(R.id.tvNomeSeguradora).text = seguradora.nome
            cardView.findViewById<TextView>(R.id.tvDescricaoSeguradora).text = seguradora.descricao
            cardView.findViewById<ImageView>(R.id.ivLogoSeguradora).setImageResource(seguradora.logoDrawableId)

            // Configura a física de arrastar para ESTE cartão
            setupSwipe(cardView)

            // Adiciona na tela
            cardContainer.addView(cardView)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupSwipe(cardView: View) {
        var dX = 0f
        var startX = 0f
        val swipeThreshold = screenWidth * 0.3f // 30% da tela para considerar arrastado

        cardView.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    dX = v.x - event.rawX
                    startX = v.x
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    // Move o cartão junto com o dedo
                    val newX = event.rawX + dX
                    v.animate().x(newX).setDuration(0).start()

                    // Inclina o cartão levemente de acordo com a posição
                    val diferenca = v.x - startX
                    v.rotation = diferenca / 20f
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val diferenca = v.x - startX
                    if (Math.abs(diferenca) > swipeThreshold) {
                        // Passou do limite: Joga o cartão para fora da tela
                        val direction = if (diferenca > 0) 1 else -1
                        removerCartaoAnimado(v, direction)
                    } else {
                        // Não passou do limite: Volta para o centro (Efeito elástico)
                        v.animate()
                            .x(startX)
                            .rotation(0f)
                            .setDuration(300)
                            .start()
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun animarCartaoDoTopo(direction: Int) {
        if (cardContainer.childCount > 0) {
            // Pega a última View adicionada (a que está no topo da pilha visual)
            val topCard = cardContainer.getChildAt(cardContainer.childCount - 1)
            removerCartaoAnimado(topCard, direction)
        } else {
            Toast.makeText(this, "Acabaram as seguradoras!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun removerCartaoAnimado(view: View, direction: Int) {
        view.animate()
            .x(screenWidth * direction * 1.5f) // Joga bem longe para fora da tela
            .alpha(0f) // Vai sumindo
            .setDuration(300)
            .withEndAction {
                cardContainer.removeView(view)
                if (direction == 1) {
                    onApprove()
                } else {
                    onReject()
                }
            }
            .start()
    }

    private fun onApprove() {
        Toast.makeText(this, "Você curtiu! Ir para detalhes...", Toast.LENGTH_SHORT).show()
        // Aqui você pode colocar o código para abrir a Activity de Detalhes (Intent)
    }

    private fun onReject() {
        // Apenas ignora. O cartão já foi removido.
    }
}