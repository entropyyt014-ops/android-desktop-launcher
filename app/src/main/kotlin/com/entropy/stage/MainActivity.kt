package com.entropy.stage

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.entropy.stage.designsystem.StageTheme
import com.entropy.stage.device.DeviceProfileDetector

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val deviceProfile = DeviceProfileDetector(this).snapshot()

        setContent {
            StageTheme {
                FoundationShell(profile = deviceProfile)
            }
        }

        window.decorView.post { reportFullyDrawn() }
    }
}
