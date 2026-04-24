package com.example.niord

import android.content.Intent
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

    private var ultimoAlerta = 0

    private var dialogAberto = false

    private val runnable = object : Runnable {
        override fun run() {
            segundosTotais++

            val horas = segundosTotais / 3600
            val minutos = (segundosTotais % 3600) / 60
            val segundos = segundosTotais % 60

            txtHoras.text = String.format("%02d", horas)
            txtMinutos.text = String.format("%02d", minutos)
            txtSegundos.text = String.format("%02d", segundos)

            if (!dialogAberto && segundosTotais - ultimoAlerta >= 900) {
                ultimoAlerta = segundosTotais
                dialogAberto = true
                showConfirmationDialog()
            }

            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.acompanhamento_medico)

        val toolbar = findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnEquipeChegou)
            .setOnClickListener {
                onBackPressedDispatcher.onBackPressed()
            }

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

    private fun showConfirmationDialog() {
        val builder = com.google.android.material.dialog.MaterialAlertDialogBuilder(this, R.style.EmergencyConfirmAlertDialog)

        builder.setTitle("A ajuda já chegou?")
            .setMessage("O tempo de espera excedeu 15 minutos. Caso a ambulância não tenha chegado, recomendamos ligar novamente para reforçar a urgência.")
            .setPositiveButton("Sim") { dialog, _ ->
                dialog.dismiss()
                dialogAberto = false
                onBackPressedDispatcher.onBackPressed()
            }
            .setNegativeButton("Ligar novamente") { dialog, _ ->
                dialog.dismiss()
                dialogAberto = false
                val callManager = CallManager()
                callManager.toCall(this, "144")
            }
            .setCancelable(false)

        builder.show()
    }

}