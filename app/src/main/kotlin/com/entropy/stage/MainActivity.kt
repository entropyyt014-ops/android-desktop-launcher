package com.entropy.stage

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.entropy.stage.designsystem.StageTheme
import com.entropy.stage.device.DeviceProfileDetector
import com.entropy.stage.shell.StageShellViewModel

class MainActivity : ComponentActivity() {
    private lateinit var stageViewModel: StageShellViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        configureDesktopInsets()

        val deviceProfile = DeviceProfileDetector(this).snapshot()
        stageViewModel = ViewModelProvider(
            this,
            StageShellViewModel.Factory(
                application = application,
                initialProfile = deviceProfile,
            ),
        )[StageShellViewModel::class.java]
        stageViewModel.updateDeviceProfile(deviceProfile)

        setContent {
            val state = stageViewModel.uiState.collectAsStateWithLifecycle().value
            StageTheme {
                StageApp(
                    state = state,
                    actions = stageViewModel,
                )
            }
        }

        window.decorView.post { reportFullyDrawn() }
    }

    override fun onResume() {
        super.onResume()
        if (::stageViewModel.isInitialized) {
            stageViewModel.refreshPlatformState()
        }
    }

    private fun configureDesktopInsets() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.statusBars())
        }
    }
}
