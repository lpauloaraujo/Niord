package com.example.niord

import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.RelativeLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.SwitchCompat
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.net.toUri
import com.example.niord.ui.theme.NiordTheme
import com.example.niord.MainOverlayButton
import com.example.niord.FloatingLifecycleOwner
import com.example.niord.Permission
import java.util.zip.Inflater

@RequiresApi(Build.VERSION_CODES.O)
class MainActivity : ComponentActivity() {
    private var permission = Permission(this)
    private var lifecycleOwner = FloatingLifecycleOwner().apply {
        onCreate()
        onResume()
    }
    private lateinit var buttonOverlay: MainOverlayButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        buttonOverlayInit()

        val inflater = LayoutInflater.from(this)
        val layout = LinearLayout(this)
        val view = inflater.inflate(R.layout.configuracao, layout, false)
        setContentView(view)

        buttonListeners()
    }

    override fun onDestroy() {
        buttonOverlay.onDestroy()
        lifecycleOwner.onDestroy()
        super.onDestroy()
    }

    fun buttonOverlayInit(){
        buttonOverlay = MainOverlayButton(this, lifecycleOwner)
        buttonOverlay.setVisibility(false)
        buttonOverlay.invoke()
    }

    fun buttonListeners(){
        findViewById<CheckBox>(R.id.checkboxDesativar).setOnCheckedChangeListener { button, bool ->
            //Permission checking
            if (!Settings.canDrawOverlays(this)) {
                permission.getOverlayPermissions{
                    if (Settings.canDrawOverlays(this)) {
                        buttonOverlay.setVisibility(bool)
                        //Invoke may fail if permission is disabled on app startup
                        buttonOverlay.invoke()
                    }
                }
            }else{
                buttonOverlay.invoke()
                buttonOverlay.setVisibility(bool)
            }
        }

        findViewById<SwitchCompat>(R.id.switchFixar).setOnCheckedChangeListener { button, bool ->
            buttonOverlay.isDraggable = !bool
        }

        findViewById<RelativeLayout>(R.id.itemLogout).setOnClickListener {
            // Mostrar dialog de confirmação de logout
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Confirmar Logout")
            builder.setMessage("Tem certeza que deseja fazer logout?")

            // Botão Confirmar
            builder.setPositiveButton("Confirmar") { dialog, which ->
                // Implementar lógica de logout aqui
                finish()
            }

            // Botão Cancelar
            builder.setNegativeButton("Cancelar") { dialog, which ->
                dialog.dismiss()
            }

            val dialog = builder.create()
            dialog.show()

            // Customizar os botões do dialog para seguir o padrão visual
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.apply {
                setTextColor(android.graphics.Color.parseColor("#4A6CF7"))
                textSize = 16f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
            }

            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.apply {
                setTextColor(android.graphics.Color.parseColor("#666666"))
                textSize = 16f
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    NiordTheme {
        Greeting("Android")
    }
}