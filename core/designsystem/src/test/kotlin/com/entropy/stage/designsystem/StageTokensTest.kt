package com.entropy.stage.designsystem

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StageTokensTest {
    @Test
    fun compactMetricsRemainPhoneReadable() {
        val metrics = stageDimensionsFor(width = 360.dp, fontScale = 1f)

        assertEquals(14.dp, metrics.contentPadding)
        assertEquals(48.dp, metrics.minTouchTarget)
        assertTrue(metrics.dockHeight >= 62.dp)
    }

    @Test
    fun fontScaleCanGrowWithoutUnboundedLayoutInflation() {
        val metrics = stageDimensionsFor(width = 360.dp, fontScale = 2f)

        assertEquals(64.8f, metrics.minTouchTarget.value, 0.01f)
    }

    @Test
    fun largerWindowsReceiveDesktopSpacing() {
        val metrics = stageDimensionsFor(width = 900.dp, fontScale = 1f)

        assertEquals(22.dp, metrics.contentPadding)
        assertEquals(296.dp, metrics.sidePanelWidth)
    }
}
