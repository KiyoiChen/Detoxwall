package com.example.enforcement

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

/**
 * Synthesised two-tone sweep siren on the ALARM stream plus vibration. While it plays the
 * accessibility service swallows volume keys and [enforceVolume] re-applies the level.
 */
class SirenPlayer(private val context: Context) {
    private val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val vibrator: Vibrator? =
        if (Build.VERSION.SDK_INT >= 31) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

    private var job: Job? = null
    private var volumePercent = 90
    private val _playing = MutableStateFlow(false)
    val playing: StateFlow<Boolean> = _playing.asStateFlow()

    fun play(scope: CoroutineScope, volumePercent: Int, seconds: Int = 8) {
        if (_playing.value) return
        this.volumePercent = volumePercent
        _playing.value = true
        enforceVolume()
        job = scope.launch(Dispatchers.Default) {
            val sound = launch { synthLoop() }
            val vibrate = launch {
                while (isActive) {
                    runCatching {
                        vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 400, 200), -1))
                    }
                    delay(600)
                }
            }
            delay(seconds * 1000L)
            sound.cancel(); vibrate.cancel()
            stop()
        }
    }

    fun stop() {
        _playing.value = false
        job?.cancel()
        job = null
        runCatching { vibrator?.cancel() }
    }

    /** Re-applies the siren volume (called when a volume key is swallowed). */
    fun enforceVolume() {
        runCatching {
            val max = audio.getStreamMaxVolume(AudioManager.STREAM_ALARM)
            val target = (max * volumePercent / 100).coerceAtLeast(1)
            audio.setStreamVolume(AudioManager.STREAM_ALARM, target, 0)
        }
    }

    private fun synthLoop() {
        val rate = 22_050
        val cycle = rate * 700 / 1000 // one 700 ms low→high→low sweep
        val buffer = ShortArray(cycle)
        var phase = 0.0
        for (i in 0 until cycle) {
            val p = i.toDouble() / cycle
            val freq = 650.0 + 700.0 * (if (p < 0.5) p * 2 else (1 - p) * 2)
            phase = (phase + 2 * PI * freq / rate) % (2 * PI)
            buffer[i] = (sin(phase) * 30_000).toInt().toShort()
        }
        val track = runCatching {
            AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(rate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(cycle * 2)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
        }.getOrNull() ?: return
        try {
            track.play()
            while (_playing.value) track.write(buffer, 0, buffer.size)
        } catch (_: Exception) {
        } finally {
            runCatching { track.stop() }
            track.release()
        }
    }
}
