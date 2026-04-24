package com.example.niord

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.os.Handler
import android.os.Looper
import android.widget.TextView

class PosEmergenciaActivity : AppCompatActivity() {

    private lateinit var txtHoras: TextView
    private lateinit var txtMinutos: TextView
    private lateinit var txtSegundos: TextView

    private val handler = Handler(Looper.getMainLooper())
    private var segundosTotais = 0

    private val runnable = object : Runnable {
        override fun run() {
            segundosTotais++

            val horas = segundosTotais / 3600
            val minutos = (segundosTotais % 3600) / 60
            val segundos = segundosTotais % 60

            txtHoras.text = String.format("%02d", horas)
            txtMinutos.text = String.format("%02d", minutos)
            txtSegundos.text = String.format("%02d", segundos)

            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.acompanhamento_medico)

        val toolbar = findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        txtHoras = findViewById(R.id.txtHoras)
        txtMinutos = findViewById(R.id.txtMinutos)
        txtSegundos = findViewById(R.id.txtSegundos)

        handler.post(runnable)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(runnable)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}