package com.example.niord

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.annotation.DrawableRes
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

class OnboardingActivity : ComponentActivity() {
    private lateinit var root: View
    private lateinit var pageImage: ImageView

    private var currentPage = 0
    private var touchStartX = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        setContentView(R.layout.onboarding)
        hideSystemBars()

        root = findViewById(R.id.onboardingRoot)
        pageImage = findViewById(R.id.imgOnboardingPage)

        setupActions()
        renderPage(0)
    }

    private fun setupActions() {
        findViewById<View>(R.id.btnSkipOnboarding).setOnClickListener {
            finishOnboarding()
        }

        root.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    touchStartX = event.x
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val delta = event.x - touchStartX
                    when {
                        delta < -SWIPE_THRESHOLD -> goForward()
                        delta > SWIPE_THRESHOLD -> goBack()
                        else -> goForward()
                    }
                    true
                }
                else -> true
            }
        }
    }

    private fun hideSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    private fun goForward() {
        if (currentPage == pages.lastIndex) {
            finishOnboarding()
        } else {
            renderPage(currentPage + 1)
        }
    }

    private fun goBack() {
        if (currentPage > 0) {
            renderPage(currentPage - 1)
        }
    }

    private fun renderPage(index: Int) {
        currentPage = index
        pageImage.setImageResource(pages[index])
    }

    private fun finishOnboarding() {
        UserFlowPreferences.setOnboardingCompleted(this, true)
        UserFlowPreferences.setOnboardingAvailable(this, false)
        UserFlowPreferences.setShowConfiguration(this, true)
        startActivity(Intent(this, ConfiguracaoActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        })
        finish()
    }

    companion object {
        private const val SWIPE_THRESHOLD = 80f

        @DrawableRes
        private val pages = intArrayOf(
            R.drawable.get_started_2,
            R.drawable.get_started_3,
            R.drawable.get_started_4,
            R.drawable.get_started_5,
            R.drawable.get_started_6,
            R.drawable.get_started_7,
            R.drawable.get_started_8
        )
    }
}
