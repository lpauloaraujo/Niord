package com.example.niord

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.widget.CheckBox
import android.widget.LinearLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatDelegate
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
import com.example.niord.CallMonitor
import java.util.zip.Inflater

@RequiresApi(Build.VERSION_CODES.O)
class MainActivity : ComponentActivity() {
    private var permission = Permission(this)
    private var lifecycleOwner = FloatingLifecycleOwner().apply {
        onCreate()
        onResume()
    }
    private var callMonitor: CallMonitor? = null
    private lateinit var buttonOverlay: MainOverlayButton


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        enableEdgeToEdge()
        buttonOverlayInit()

        buttonOverlay.onCallClick = { number ->
            showCallDialog(number)
        }

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
    }

    private fun showCallDialog(number: String) {

        val title: String
        val message: String
        val positiveText: String
        val negativeText: String

        when (number) {

            "144" -> {
                title = "Ligar para Emergência?"
                message = "Você será direcionado para a chamada telefônica. Confirme para discar imediatamente."
                positiveText = "Ligar Agora"
                negativeText = "Cancelar"
            }

            else -> {
                title = "Chamada"
                message = "Deseja realmente ligar para $number?"
                positiveText = "Confirmar"
                negativeText = "Cancelar"
            }
        }

        val dialog = com.google.android.material.dialog.MaterialAlertDialogBuilder(
            this,
            R.style.CustomAlertDialog
        )
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(positiveText) { _, _ ->

                permission.requestCallAndPhoneStatePermission { granted ->
                    if (granted) {

                        // 🔹 cria o monitor
                        callMonitor = CallMonitor(
                            context = this,
                            onCallStarted = {
                                runOnUiThread {
                                }
                            },
                            onCallEnded = {
                                runOnUiThread {

                                    callMonitor?.stop()
                                    callMonitor = null

                                    runOnUiThread {
                                        val intent = Intent(this, PosEmergenciaActivity::class.java)
                                        startActivity(intent)
                                    }
                                }
                            }
                        )

                        callMonitor?.start()

                        CallManager().toCall(this, number)

                    }
                }
            }
            .setNegativeButton(negativeText, null)
            .create()

        dialog.show()
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