package com.entropy.stage.device

enum class StageWindowClass {
    COMPACT,
    MEDIUM,
    EXPANDED,
}

enum class DevicePosture {
    PHONE_PORTRAIT,
    PHONE_LANDSCAPE,
    LARGE_SCREEN,
}

enum class PerformanceTier {
    LEAN,
    BALANCED,
    EXPANDED,
}

data class InputProfile(
    val hasTouchscreen: Boolean,
    val hasMouse: Boolean,
    val hasPhysicalKeyboard: Boolean,
)

data class PerformanceBudget(
    val allowContinuousBlur: Boolean,
    val maxLiveWebViews: Int,
    val maxSimultaneousWindows: Int,
)

data class DeviceProfile(
    val manufacturer: String,
    val model: String,
    val sdkInt: Int,
    val widthPx: Int,
    val heightPx: Int,
    val widthDp: Int,
    val heightDp: Int,
    val smallestWidthDp: Int,
    val densityDpi: Int,
    val fontScale: Float,
    val totalRamMb: Long,
    val appMemoryClassMb: Int,
    val maxHeapMb: Long,
    val windowClass: StageWindowClass,
    val posture: DevicePosture,
    val performanceTier: PerformanceTier,
    val input: InputProfile,
) {
    val budget: PerformanceBudget
        get() = DeviceProfileClassifier.performanceBudget(performanceTier)

    companion object {
        fun preview(
            widthDp: Int = 360,
            heightDp: Int = 740,
            totalRamMb: Long = 4_096,
            hasMouse: Boolean = false,
            hasPhysicalKeyboard: Boolean = false,
        ): DeviceProfile {
            val tier = DeviceProfileClassifier.performanceTier(
                totalRamMb = totalRamMb,
                appMemoryClassMb = 256,
            )
            return DeviceProfile(
                manufacturer = "Samsung",
                model = "Galaxy A30 reference",
                sdkInt = 30,
                widthPx = widthDp * 3,
                heightPx = heightDp * 3,
                widthDp = widthDp,
                heightDp = heightDp,
                smallestWidthDp = minOf(widthDp, heightDp),
                densityDpi = 420,
                fontScale = 1f,
                totalRamMb = totalRamMb,
                appMemoryClassMb = 256,
                maxHeapMb = 256,
                windowClass = DeviceProfileClassifier.windowClass(widthDp),
                posture = DeviceProfileClassifier.posture(widthDp, heightDp),
                performanceTier = tier,
                input = InputProfile(
                    hasTouchscreen = true,
                    hasMouse = hasMouse,
                    hasPhysicalKeyboard = hasPhysicalKeyboard,
                ),
            )
        }
    }
}

object DeviceProfileClassifier {
    fun windowClass(widthDp: Int): StageWindowClass = when {
        widthDp < 600 -> StageWindowClass.COMPACT
        widthDp < 840 -> StageWindowClass.MEDIUM
        else -> StageWindowClass.EXPANDED
    }

    fun posture(widthDp: Int, heightDp: Int): DevicePosture = when {
        minOf(widthDp, heightDp) >= 600 -> DevicePosture.LARGE_SCREEN
        widthDp > heightDp -> DevicePosture.PHONE_LANDSCAPE
        else -> DevicePosture.PHONE_PORTRAIT
    }

    fun performanceTier(
        totalRamMb: Long,
        appMemoryClassMb: Int,
    ): PerformanceTier {
        val usableRamMb = totalRamMb.takeIf { it > 0 }
        return when {
            usableRamMb != null && usableRamMb <= 5_120 -> PerformanceTier.LEAN
            usableRamMb != null && usableRamMb <= 8_192 -> PerformanceTier.BALANCED
            usableRamMb != null -> PerformanceTier.EXPANDED
            appMemoryClassMb <= 256 -> PerformanceTier.LEAN
            appMemoryClassMb <= 512 -> PerformanceTier.BALANCED
            else -> PerformanceTier.EXPANDED
        }
    }

    fun performanceBudget(tier: PerformanceTier): PerformanceBudget = when (tier) {
        PerformanceTier.LEAN -> PerformanceBudget(
            allowContinuousBlur = false,
            maxLiveWebViews = 2,
            maxSimultaneousWindows = 2,
        )

        PerformanceTier.BALANCED -> PerformanceBudget(
            allowContinuousBlur = false,
            maxLiveWebViews = 3,
            maxSimultaneousWindows = 3,
        )

        PerformanceTier.EXPANDED -> PerformanceBudget(
            allowContinuousBlur = true,
            maxLiveWebViews = 4,
            maxSimultaneousWindows = 4,
        )
    }
}
