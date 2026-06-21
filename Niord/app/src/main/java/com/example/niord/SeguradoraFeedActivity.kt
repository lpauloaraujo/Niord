package com.example.niord

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlin.math.abs

class SeguradoraFeedActivity : AppCompatActivity() {

    private lateinit var cardContainer: FrameLayout
    private lateinit var bottomActionsContainer: View
    private lateinit var emptyStateLayout: View

    private lateinit var btnRejeitar: FloatingActionButton
    private lateinit var btnAprovar: FloatingActionButton

    private var screenWidth = 0f
    private var isAnimating = false

    private val swipeThreshold by lazy {
        screenWidth * 0.28f
    }

    private val seguradoras = listOf(
        Seguradora(
            id = 1,
            nome = "Forti-Mora",
            descricao = "Voltada para motociclistas que buscam proteção no dia a dia, com foco em agilidade, segurança e assistência prática.",
            logoDrawableId = R.drawable.ic_launcher_foreground
        ),
        Seguradora(
            id = 2,
            nome = "Gran-Vox",
            descricao = "Pensada para entregadores e motoristas que precisam de cobertura acessível, flexível e adequada à rotina de trabalho.",
            logoDrawableId = R.drawable.ic_launcher_foreground
        ),
        Seguradora(
            id = 3,
            nome = "Segura-Lastit",
            descricao = "Especializada em carros, oferecendo soluções para quem deseja mais tranquilidade, suporte e proteção veicular.",
            logoDrawableId = R.drawable.ic_launcher_foreground
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_seguradora_feed)

        inicializarViews()
        configurarAcoes()
        carregarSeguradoras()
    }

    private fun inicializarViews() {
        cardContainer = findViewById(R.id.cardContainer)
        bottomActionsContainer = findViewById(R.id.bottomActionsContainer)
        emptyStateLayout = findViewById(R.id.emptyStateLayout)

        btnRejeitar = findViewById(R.id.btnRejeitar)
        btnAprovar = findViewById(R.id.btnAprovar)

        screenWidth = resources.displayMetrics.widthPixels.toFloat()
    }

    private fun configurarAcoes() {
        findViewById<ImageButton>(R.id.btnVoltar).setOnClickListener {
            finish()
        }

        btnRejeitar.setOnClickListener {
            animarCartaoDoTopo(direction = DIRECTION_LEFT)
        }

        btnAprovar.setOnClickListener {
            animarCartaoDoTopo(direction = DIRECTION_RIGHT)
        }
    }

    private fun carregarSeguradoras() {
        cardContainer.removeAllViews()

        seguradoras.reversed().forEach { seguradora ->
            val cardView = LayoutInflater.from(this)
                .inflate(R.layout.item_seguradora_card, cardContainer, false)

            cardView.tag = seguradora

            preencherCard(cardView, seguradora)
            configurarSwipe(cardView)

            cardContainer.addView(cardView)
        }

        atualizarVisualDaPilha()
        atualizarEstadoDaTela()
    }

    private fun preencherCard(cardView: View, seguradora: Seguradora) {
        cardView.findViewById<TextView>(R.id.tvNomeSeguradora).text = seguradora.nome
        cardView.findViewById<TextView>(R.id.tvDescricaoSeguradora).text = seguradora.descricao

        cardView.findViewById<ImageView>(R.id.ivLogoSeguradora)
            .setImageResource(seguradora.logoDrawableId)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun configurarSwipe(cardView: View) {
        var initialX = 0f
        var initialY = 0f
        var touchOffsetX = 0f
        var touchOffsetY = 0f

        cardView.setOnTouchListener { view, event ->
            if (isAnimating || !isTopCard(view)) return@setOnTouchListener false

            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = view.x
                    initialY = view.y
                    touchOffsetX = view.x - event.rawX
                    touchOffsetY = view.y - event.rawY

                    view.animate().cancel()
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    val newX = event.rawX + touchOffsetX
                    val newY = event.rawY + touchOffsetY

                    val dragDistance = newX - initialX
                    val rotation = dragDistance / ROTATION_FACTOR

                    view.x = newX
                    view.y = newY
                    view.rotation = rotation.coerceIn(-MAX_ROTATION, MAX_ROTATION)

                    true
                }

                MotionEvent.ACTION_UP,
                MotionEvent.ACTION_CANCEL -> {
                    val dragDistance = view.x - initialX

                    if (abs(dragDistance) > swipeThreshold) {
                        val direction = if (dragDistance > 0) DIRECTION_RIGHT else DIRECTION_LEFT
                        removerCartaoAnimado(view, direction)
                    } else {
                        retornarCartaoParaCentro(view, initialX, initialY)
                    }

                    true
                }

                else -> false
            }
        }
    }

    private fun animarCartaoDoTopo(direction: Int) {
        if (isAnimating) return

        val topCard = getTopCard()

        if (topCard == null) {
            mostrarEstadoVazio()
            return
        }

        removerCartaoAnimado(topCard, direction)
    }

    private fun removerCartaoAnimado(view: View, direction: Int) {
        isAnimating = true
        atualizarEstadoDosBotoes()

        val seguradora = view.tag as? Seguradora
        val targetX = screenWidth * direction * 1.4f
        val targetRotation = direction * 18f

        view.animate()
            .x(targetX)
            .alpha(0f)
            .rotation(targetRotation)
            .setDuration(ANIMATION_DURATION)
            .withEndAction {
                cardContainer.removeView(view)

                if (direction == DIRECTION_RIGHT) {
                    onApprove(seguradora)
                } else {
                    onReject(seguradora)
                }

                isAnimating = false
                atualizarVisualDaPilha()
                atualizarEstadoDaTela()
            }
            .start()
    }

    private fun retornarCartaoParaCentro(view: View, initialX: Float, initialY: Float) {
        isAnimating = true
        atualizarEstadoDosBotoes()

        view.animate()
            .x(initialX)
            .y(initialY)
            .rotation(0f)
            .alpha(1f)
            .setDuration(ANIMATION_DURATION)
            .setInterpolator(OvershootInterpolator(1.2f))
            .withEndAction {
                isAnimating = false
                atualizarEstadoDaTela()
            }
            .start()
    }

    private fun atualizarVisualDaPilha() {
        val totalCards = cardContainer.childCount

        for (index in 0 until totalCards) {
            val card = cardContainer.getChildAt(index)
            val positionFromTop = totalCards - 1 - index

            val scale = when (positionFromTop) {
                0 -> 1f
                1 -> 0.96f
                2 -> 0.92f
                else -> 0.9f
            }

            val translationY = when (positionFromTop) {
                0 -> 0f
                1 -> 14f
                2 -> 28f
                else -> 36f
            }

            card.animate()
                .scaleX(scale)
                .scaleY(scale)
                .translationY(translationY)
                .alpha(if (positionFromTop > 2) 0f else 1f)
                .setDuration(200)
                .start()
        }
    }

    private fun atualizarEstadoDaTela() {
        val temCards = cardContainer.childCount > 0

        if (temCards) {
            mostrarEstadoComCards()
        } else {
            mostrarEstadoVazio()
        }

        atualizarEstadoDosBotoes()
    }

    private fun mostrarEstadoComCards() {
        emptyStateLayout.animate().cancel()
        bottomActionsContainer.animate().cancel()

        emptyStateLayout.visibility = View.GONE
        emptyStateLayout.alpha = 0f

        bottomActionsContainer.visibility = View.VISIBLE
        bottomActionsContainer.translationY = 0f
        bottomActionsContainer.animate()
            .alpha(1f)
            .setDuration(200)
            .start()
    }

    private fun mostrarEstadoVazio() {
        bottomActionsContainer.animate().cancel()
        emptyStateLayout.animate().cancel()

        btnRejeitar.isEnabled = false
        btnAprovar.isEnabled = false

        bottomActionsContainer.animate()
            .alpha(0f)
            .translationY(80f)
            .setDuration(220)
            .withEndAction {
                bottomActionsContainer.visibility = View.GONE
                bottomActionsContainer.translationY = 0f
            }
            .start()

        emptyStateLayout.visibility = View.VISIBLE
        emptyStateLayout.translationY = 24f
        emptyStateLayout.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(260)
            .start()
    }

    private fun atualizarEstadoDosBotoes() {
        val enabled = cardContainer.childCount > 0 && !isAnimating

        btnRejeitar.isEnabled = enabled
        btnAprovar.isEnabled = enabled

        btnRejeitar.alpha = if (enabled) 1f else 0.45f
        btnAprovar.alpha = if (enabled) 1f else 0.45f
    }

    private fun getTopCard(): View? {
        return if (cardContainer.childCount > 0) {
            cardContainer.getChildAt(cardContainer.childCount - 1)
        } else {
            null
        }
    }

    private fun isTopCard(view: View): Boolean {
        return view == getTopCard()
    }

    private fun onApprove(seguradora: Seguradora?) {
        val nome = seguradora?.nome ?: "Seguradora"
        Toast.makeText(this, "Você aprovou $nome", Toast.LENGTH_SHORT).show()
    }

    private fun onReject(seguradora: Seguradora?) {
        // Rejeição registrada ou ignorada.
    }

    companion object {
        private const val DIRECTION_LEFT = -1
        private const val DIRECTION_RIGHT = 1

        private const val ANIMATION_DURATION = 280L
        private const val ROTATION_FACTOR = 22f
        private const val MAX_ROTATION = 14f
    }
}