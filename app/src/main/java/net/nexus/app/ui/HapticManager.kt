package net.nexus.app.ui

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import net.nexus.app.data.HapticIntensity

enum class HapticType {
    TICK,
    CLICK,
    TOGGLE,
    SUCCESS,
    ERROR
}

object HapticManager {

    fun trigger(context: Context, intensity: HapticIntensity, type: HapticType = HapticType.CLICK) {
        if (intensity == HapticIntensity.OFF) return

        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vm?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        } ?: return

        if (!vibrator.hasVibrator()) return

        val multiplier = when (intensity) {
            HapticIntensity.OFF -> 0.0f
            HapticIntensity.LIGHT -> 0.45f
            HapticIntensity.MEDIUM -> 0.75f
            HapticIntensity.STRONG -> 1.0f
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (type == HapticType.SUCCESS) {
                // Crisp double micro-pulse on successful connection confirmation
                val timings = longArrayOf(0, 28, 65, 36)
                val baseAmps = intArrayOf(0, 190, 0, 255)
                val amps = baseAmps.map { (it * multiplier).toInt().coerceIn(0, 255) }.toIntArray()
                if (vibrator.hasAmplitudeControl()) {
                    vibrator.vibrate(VibrationEffect.createWaveform(timings, amps, -1))
                } else {
                    vibrator.vibrate(VibrationEffect.createWaveform(timings, -1))
                }
                return
            }

            val (durationMs, baseAmp) = when (type) {
                HapticType.TICK -> 12L to 70
                HapticType.CLICK -> 22L to 140
                HapticType.TOGGLE -> 35L to 200
                HapticType.SUCCESS -> 50L to 255
                HapticType.ERROR -> 70L to 255
            }
            val amp = (baseAmp * multiplier).toInt().coerceIn(1, 255)
            if (vibrator.hasAmplitudeControl()) {
                vibrator.vibrate(VibrationEffect.createOneShot(durationMs, amp))
            } else {
                vibrator.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
            }
        } else {
            if (type == HapticType.SUCCESS) {
                @Suppress("DEPRECATION")
                vibrator.vibrate(longArrayOf(0, 28, 65, 36), -1)
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(25L)
            }
        }
    }
}
