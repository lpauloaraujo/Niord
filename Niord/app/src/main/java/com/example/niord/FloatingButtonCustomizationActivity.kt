package com.example.niord

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import com.google.android.material.button.MaterialButton
import com.google.android.material.slider.Slider

class FloatingButtonCustomizationActivity : ComponentActivity() {

    private lateinit var ivPreview: ImageView
    private lateinit var sliderTransparency: Slider
    private lateinit var sliderSize: Slider
    private lateinit var ivColor1: ImageView
    private lateinit var ivColor2: ImageView
    private lateinit var ivColor3: ImageView
    private lateinit var ivColor4: ImageView

    private var currentTransparency: Float = 1.0f
    private var currentSize: Float = 64f
    private var currentColorIndex: Int = 0
    private var wasEnabled: Boolean = false

    // Defina aqui os nomes reais dos seus novos arquivos PNG
    private val buttonDrawables = intArrayOf(
        R.drawable.main_button,
        R.drawable.main_button_red,
        R.drawable.main_button_purple,
        R.drawable.main_button_green
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.floating_button_customization)

        ivPreview = findViewById(R.id.ivPreview)
        sliderTransparency = findViewById(R.id.sliderTransparency)
        sliderSize = findViewById(R.id.sliderSize)
        ivColor1 = findViewById(R.id.ivColor1)
        ivColor2 = findViewById(R.id.ivColor2)
        ivColor3 = findViewById(R.id.ivColor3)
        ivColor4 = findViewById(R.id.ivColor4)

        wasEnabled = UserFlowPreferences.isOverlayEnabled(this)
        currentTransparency = UserFlowPreferences.getOverlayTransparency(this)
        currentSize = UserFlowPreferences.getOverlaySize(this)
        currentColorIndex = UserFlowPreferences.getOverlayColorIndex(this)

        if (wasEnabled) {
            UserFlowPreferences.setOverlayEnabled(this, false)
            sendBroadcast(Intent("com.example.niord.UPDATE_OVERLAY"))
        }

        setupUI()
        updatePreview()

        findViewById<android.view.View>(R.id.btnBack).setOnClickListener {
            saveAndExit()
        }

        findViewById<MaterialButton>(R.id.btnRestore).setOnClickListener {
            restoreDefaults()
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                saveAndExit()
            }
        })
    }

    private fun setupUI() {
        sliderTransparency.value = currentTransparency.coerceIn(0.3f, 1.0f)
        sliderTransparency.addOnChangeListener { _, value, _ ->
            currentTransparency = value
            updatePreview()
        }

        sliderSize.value = currentSize.coerceIn(48f, 96f)
        sliderSize.addOnChangeListener { _, value, _ ->
            currentSize = value
            updatePreview()
        }

        ivColor1.setOnClickListener { currentColorIndex = 0; updatePreview() }
        ivColor2.setOnClickListener { currentColorIndex = 1; updatePreview() }
        ivColor3.setOnClickListener { currentColorIndex = 2; updatePreview() }
        ivColor4.setOnClickListener { currentColorIndex = 3; updatePreview() }
        
        // Opcional: Você pode colocar os PNGs correspondentes nos seletores também
        ivColor1.setImageResource(buttonDrawables[0])
        ivColor2.setImageResource(buttonDrawables[1])
        ivColor3.setImageResource(buttonDrawables[2])
        ivColor4.setImageResource(buttonDrawables[3])
    }

    private fun updatePreview() {
        ivPreview.alpha = currentTransparency
        ivPreview.setImageResource(buttonDrawables[currentColorIndex])

        val params = ivPreview.layoutParams
        val pxSize = (currentSize * resources.displayMetrics.density).toInt()
        params.width = pxSize
        params.height = pxSize
        ivPreview.layoutParams = params
    }

    private fun saveAndExit() {
        UserFlowPreferences.setOverlayTransparency(this, currentTransparency)
        UserFlowPreferences.setOverlaySize(this, currentSize)
        UserFlowPreferences.setOverlayColorIndex(this, currentColorIndex)
        
        if (wasEnabled) {
            UserFlowPreferences.setOverlayEnabled(this, true)
        }
        
        sendBroadcast(Intent("com.example.niord.UPDATE_OVERLAY"))
        finish()
    }

    private fun restoreDefaults() {
        currentTransparency = 1.0f
        currentSize = 64f
        currentColorIndex = 0
        
        sliderTransparency.value = currentTransparency
        sliderSize.value = currentSize
        updatePreview()
    }
}
