package com.entropy.stage.device

import android.app.ActivityManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.view.InputDevice

class DeviceProfileDetector(
    private val context: Context,
) {
    fun snapshot(): DeviceProfile {
        val resources = context.resources
        val configuration = resources.configuration
        val displayMetrics = resources.displayMetrics
        val activityManager = context.getSystemService(ActivityManager::class.java)
        val memoryInfo = ActivityManager.MemoryInfo().also(activityManager::getMemoryInfo)

        val widthDp = configuration.screenWidthDp.coerceAtLeast(1)
        val heightDp = configuration.screenHeightDp.coerceAtLeast(1)
        val totalRamMb = memoryInfo.totalMem / BYTES_PER_MEBIBYTE
        val memoryClassMb = activityManager.memoryClass
        val tier = DeviceProfileClassifier.performanceTier(
            totalRamMb = totalRamMb,
            appMemoryClassMb = memoryClassMb,
        )

        return DeviceProfile(
            manufacturer = Build.MANUFACTURER.toDisplayName(),
            model = Build.MODEL.trim(),
            sdkInt = Build.VERSION.SDK_INT,
            widthPx = displayMetrics.widthPixels,
            heightPx = displayMetrics.heightPixels,
            widthDp = widthDp,
            heightDp = heightDp,
            smallestWidthDp = configuration.smallestScreenWidthDp,
            densityDpi = configuration.densityDpi,
            fontScale = configuration.fontScale,
            totalRamMb = totalRamMb,
            appMemoryClassMb = memoryClassMb,
            maxHeapMb = Runtime.getRuntime().maxMemory() / BYTES_PER_MEBIBYTE,
            windowClass = DeviceProfileClassifier.windowClass(widthDp),
            posture = DeviceProfileClassifier.posture(widthDp, heightDp),
            performanceTier = tier,
            input = detectInput(configuration),
        )
    }

    private fun detectInput(configuration: Configuration): InputProfile {
        val devices: List<InputDevice> = InputDevice.getDeviceIds()
            .asSequence()
            .map { deviceId -> InputDevice.getDevice(deviceId) }
            .filterNotNull()
            .toList()

        return InputProfile(
            hasTouchscreen = configuration.touchscreen != Configuration.TOUCHSCREEN_NOTOUCH,
            hasMouse = devices.any { device ->
                device.sources and InputDevice.SOURCE_MOUSE == InputDevice.SOURCE_MOUSE
            },
            hasPhysicalKeyboard = devices.any { device ->
                !device.isVirtual &&
                    device.keyboardType == InputDevice.KEYBOARD_TYPE_ALPHABETIC
            },
        )
    }

    private fun String.toDisplayName(): String =
        trim().replaceFirstChar { character ->
            if (character.isLowerCase()) character.titlecase() else character.toString()
        }

    private companion object {
        const val BYTES_PER_MEBIBYTE = 1_048_576L
    }
}
