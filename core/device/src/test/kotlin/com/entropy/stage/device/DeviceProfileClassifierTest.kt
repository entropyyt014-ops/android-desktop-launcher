package com.entropy.stage.device

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class DeviceProfileClassifierTest {
    @Test
    fun compactPortraitMatchesReferencePhone() {
        assertEquals(StageWindowClass.COMPACT, DeviceProfileClassifier.windowClass(360))
        assertEquals(
            DevicePosture.PHONE_PORTRAIT,
            DeviceProfileClassifier.posture(widthDp = 360, heightDp = 740),
        )
    }

    @Test
    fun compactLandscapeUsesLandscapePosture() {
        assertEquals(
            DevicePosture.PHONE_LANDSCAPE,
            DeviceProfileClassifier.posture(widthDp = 740, heightDp = 360),
        )
    }

    @Test
    fun fourGigabytePhoneSelectsLeanBudget() {
        val tier = DeviceProfileClassifier.performanceTier(
            totalRamMb = 3_840,
            appMemoryClassMb = 256,
        )

        assertEquals(PerformanceTier.LEAN, tier)
        assertFalse(DeviceProfileClassifier.performanceBudget(tier).allowContinuousBlur)
        assertEquals(2, DeviceProfileClassifier.performanceBudget(tier).maxLiveWebViews)
    }

    @Test
    fun largerMemoryProfilesScaleWithoutChangingWindowLogic() {
        assertEquals(
            PerformanceTier.BALANCED,
            DeviceProfileClassifier.performanceTier(
                totalRamMb = 6_144,
                appMemoryClassMb = 384,
            ),
        )
        assertEquals(
            PerformanceTier.EXPANDED,
            DeviceProfileClassifier.performanceTier(
                totalRamMb = 12_288,
                appMemoryClassMb = 768,
            ),
        )
        assertEquals(StageWindowClass.EXPANDED, DeviceProfileClassifier.windowClass(900))
    }
}
