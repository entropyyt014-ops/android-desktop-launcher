package com.entropy.stage

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.entropy.stage.designsystem.StageTheme
import com.entropy.stage.device.DeviceProfile

@PreviewTest
@Preview(
    name = "Galaxy A30 portrait",
    widthDp = 360,
    heightDp = 740,
    showBackground = true,
)
@Composable
fun foundationPortraitScreenshot() {
    StageTheme {
        FoundationShell(
            profile = DeviceProfile.preview(
                widthDp = 360,
                heightDp = 740,
            ),
        )
    }
}

@PreviewTest
@Preview(
    name = "Phone desktop landscape",
    widthDp = 740,
    heightDp = 360,
    showBackground = true,
)
@Composable
fun foundationLandscapeScreenshot() {
    StageTheme {
        FoundationShell(
            profile = DeviceProfile.preview(
                widthDp = 740,
                heightDp = 360,
                hasMouse = true,
                hasPhysicalKeyboard = true,
            ),
        )
    }
}
