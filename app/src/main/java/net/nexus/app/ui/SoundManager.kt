package net.nexus.app.ui

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

/**
 * Procedural low-latency sound generator for connection feedback and easter eggs.
 * Zero external audio assets, zero file bloat, clean mathematical PCM synthesis.
 */
object SoundManager {

    private val scope = CoroutineScope(Dispatchers.Default)

    /**
     * Futuristic sci-fi rising chime on successful connection.
     */
    fun playConnect(enabled: Boolean) {
        if (!enabled) return
        scope.launch {
            runCatching {
                val sampleRate = 44100
                val durationMs = 200
                val numSamples = (sampleRate * (durationMs / 1000f)).toInt()
                val samples = ShortArray(numSamples)

                for (i in 0 until numSamples) {
                    val progress = i.toFloat() / numSamples
                    // Pitch glide: 520 Hz (C5) up to 880 Hz (A5)
                    val freq = 520.0 + 360.0 * (1.0 - (1.0 - progress) * (1.0 - progress))
                    val attack = (progress * 20f).coerceAtMost(1f)
                    val decay = (1.0 - progress)
                    val envelope = attack * decay * decay

                    val wave = sin(2.0 * PI * freq * i / sampleRate) +
                               0.3 * sin(4.0 * PI * freq * i / sampleRate)
                    samples[i] = (wave * envelope * 24000.0).toInt().coerceIn(-32767, 32767).toShort()
                }

                playPcm(samples, sampleRate)
            }
        }
    }

    /**
     * Smooth sci-fi power-down click on disconnect.
     */
    fun playDisconnect(enabled: Boolean) {
        if (!enabled) return
        scope.launch {
            runCatching {
                val sampleRate = 44100
                val durationMs = 160
                val numSamples = (sampleRate * (durationMs / 1000f)).toInt()
                val samples = ShortArray(numSamples)

                for (i in 0 until numSamples) {
                    val progress = i.toFloat() / numSamples
                    // Pitch glide downward: 560 Hz down to 240 Hz
                    val freq = 560.0 - 320.0 * progress
                    val attack = (progress * 25f).coerceAtMost(1f)
                    val decay = (1.0 - progress)
                    val envelope = attack * decay

                    val wave = sin(2.0 * PI * freq * i / sampleRate)
                    samples[i] = (wave * envelope * 22000.0).toInt().coerceIn(-32767, 32767).toShort()
                }

                playPcm(samples, sampleRate)
            }
        }
    }

    /**
     * Cute cat paw meow / chirp for secret easter egg theme.
     */
    fun playCatPaw(enabled: Boolean) {
        if (!enabled) return
        scope.launch {
            runCatching {
                val sampleRate = 44100
                val durationMs = 280
                val numSamples = (sampleRate * (durationMs / 1000f)).toInt()
                val samples = ShortArray(numSamples)

                for (i in 0 until numSamples) {
                    val progress = i.toFloat() / numSamples
                    // Cat meow pitch curve: gentle rise to peak then soft release
                    val freq = if (progress < 0.45f) {
                        580.0 + 340.0 * (progress / 0.45f)
                    } else {
                        920.0 - 280.0 * ((progress - 0.45f) / 0.55f)
                    }
                    val attack = (progress * 15f).coerceAtMost(1f)
                    val decay = (1.0 - progress)
                    val envelope = attack * decay * decay

                    val wave = 0.8 * sin(2.0 * PI * freq * i / sampleRate) +
                               0.35 * sin(4.0 * PI * freq * i / sampleRate) +
                               0.15 * sin(6.0 * PI * freq * i / sampleRate)
                    samples[i] = (wave * envelope * 25000.0).toInt().coerceIn(-32767, 32767).toShort()
                }

                playPcm(samples, sampleRate)
            }
        }
    }

    private fun playPcm(samples: ShortArray, sampleRate: Int) {
        val audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(samples.size * 2)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        audioTrack.write(samples, 0, samples.size)
        audioTrack.play()
        Thread.sleep((samples.size * 1000L / sampleRate) + 40L)
        audioTrack.release()
    }
}

