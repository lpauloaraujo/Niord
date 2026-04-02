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

        buttonOverlay.onEmergencyClick = { number ->

            permission.requestCallPermission { granted ->
                if (granted) {
                    CallManager().toCall(this, number)
                }
            }

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