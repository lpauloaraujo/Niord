package com.example.niord

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.niord.ui.theme.NiordTheme
import android.os.Build
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.net.toUri





class MainActivity : ComponentActivity() {
    private lateinit var overlayManager: OverlayManager
    private lateinit var overlayManager2: OverlayManager

    @RequiresApi(Build.VERSION_CODES.O)
    //Overlay permission trigger
    private val permissionLauncherShowOverlay = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        // After returning from settings, check if permission is granted
        if (Settings.canDrawOverlays(this)) {
            overlayManager.show()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NiordTheme {
                Scaffold( modifier = Modifier.fillMaxSize() ) { innerPadding ->
                    Column(modifier = Modifier.padding(innerPadding)){
                        Greeting(
                            name = "Luz"
                        )
                        //Overlay Demo
                        Button(onClick = {
                            if (Settings.canDrawOverlays(applicationContext)) {
                                if(overlayManager.showing()){
                                    overlayManager.hide()
                                }else {
                                    overlayManager.show()
                                }
                            } else {
                                // Open settings to grant permission
                                val intent = Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    "package:$packageName".toUri()
                                )
                                permissionLauncherShowOverlay.launch(intent)
                            }
                        }) {
                            Text(
                                text = "Hi"
                            )
                        }
                    }
                }
            }
        }
        overlayManager = OverlayManager(this)
        overlayManager2 = ExampleCustomOverlay(this)
        overlayManager2.show()

    }
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onDestroy() {
        overlayManager.hide() // Always remove the view when activity is destroyed
        super.onDestroy()
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
