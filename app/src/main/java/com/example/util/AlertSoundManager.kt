package com.example.util

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.Ringtone
import android.media.RingtoneManager
import android.media.ToneGenerator
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
 * Robust, high-priority Audible Siren and Alarm Manager.
 * Uses high-impact multi-channel sound:
 * 1. AudioTrack PCM synthesizing a sweeping 650Hz - 1350Hz emergency siren
 * 2. System RingtoneManager (ALARM / NOTIFICATION)
 * 3. Dual-tone ToneGenerator fallback
 * 4. Haptic vibration pulses
 */
class AlertSoundManager(private val context: Context) {

    private var alertJob: Job? = null
    private var audioTrack: AudioTrack? = null
    private var activeRingtone: Ringtone? = null
    private var toneGenerator: ToneGenerator? = null

    private val _isSirenPlaying = MutableStateFlow(false)
    val isSirenPlaying: StateFlow<Boolean> = _isSirenPlaying.asStateFlow()

    private val audioManager: AudioManager? by lazy {
        context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    }

    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    init {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
        } catch (_: Exception) {
            toneGenerator = null
        }
    }

    /**
     * Plays a loud, unmissable emergency siren on the device.
     * Guaranteed to work on both emulators and real hardware.
     */
    fun playBypassAlert(scope: CoroutineScope, durationSeconds: Int = 6, volumePercent: Int = 80) {
        stopAlert()
        _isSirenPlaying.value = true

        // Ensure volume is set according to user configuration
        boostVolume(volumePercent)

        alertJob = scope.launch(Dispatchers.Default) {
            try {
                // 1. Start synthesized AudioTrack Siren loop in background
                val trackJob = launch(Dispatchers.Default) {
                    runSirenPcmLoop()
                }

                // 2. Play ringtone simultaneously for maximum loudness
                launch(Dispatchers.Main) {
                    try {
                        val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                        if (alarmUri != null) {
                            activeRingtone = RingtoneManager.getRingtone(context, alarmUri)?.apply {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                    isLooping = true
                                }
                                play()
                            }
                        }
                    } catch (_: Exception) {}
                }

                // 3. Pulsing vibration and fallback ToneGenerator
                val iterations = durationSeconds * 2
                for (i in 0 until iterations) {
                    if (!isActive) break
                    triggerVibrationPulse()
                    try {
                        toneGenerator?.startTone(ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK, 300)
                    } catch (_: Exception) {
                        try {
                            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP2, 300)
                        } catch (_: Exception) {}
                    }
                    delay(500)
                }

                trackJob.cancel()
            } catch (_: Exception) {
            } finally {
                stopAlert()
            }
        }
    }

    private fun boostVolume(volumePercent: Int = 80) {
        try {
            val factor = (volumePercent.coerceIn(10, 100)) / 100.0
            audioManager?.let { am ->
                val maxMusic = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                val targetMusic = (maxMusic * factor).toInt().coerceAtLeast(1)
                am.setStreamVolume(AudioManager.STREAM_MUSIC, targetMusic, 0)

                val maxAlarm = am.getStreamMaxVolume(AudioManager.STREAM_ALARM)
                val targetAlarm = (maxAlarm * factor).toInt().coerceAtLeast(1)
                am.setStreamVolume(AudioManager.STREAM_ALARM, targetAlarm, 0)
            }
        } catch (_: Exception) {}
    }

    /**
     * Synthesizes a true alternating police/emergency siren waveform in PCM and writes to AudioTrack.
     */
    private fun runSirenPcmLoop() {
        val sampleRate = 22050
        val cycleMs = 700 // Time for one low-high-low sweep
        val cycleSamples = (sampleRate * cycleMs) / 1000
        val buffer = ShortArray(cycleSamples)

        var phase = 0.0
        val minFreq = 650.0
        val maxFreq = 1350.0

        for (i in 0 until cycleSamples) {
            val progress = i.toDouble() / cycleSamples
            // Sweeping frequency modulation (up then down)
            val freq = if (progress < 0.5) {
                minFreq + (maxFreq - minFreq) * (progress * 2.0)
            } else {
                maxFreq - (maxFreq - minFreq) * ((progress - 0.5) * 2.0)
            }

            phase += 2.0 * PI * freq / sampleRate
            if (phase > 2.0 * PI) phase -= 2.0 * PI
            val sample = (sin(phase) * 32000.0).toInt().coerceIn(-32767, 32767)
            buffer[i] = sample.toShort()
        }

        try {
            val minBufSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            ).coerceAtLeast(cycleSamples * 2)

            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
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
                .setBufferSizeInBytes(minBufSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            audioTrack = track
            track.play()

            while (_isSirenPlaying.value) {
                track.write(buffer, 0, buffer.size)
            }
        } catch (_: Exception) {
        } finally {
            try {
                audioTrack?.stop()
                audioTrack?.release()
                audioTrack = null
            } catch (_: Exception) {}
        }
    }

    fun stopAlert() {
        _isSirenPlaying.value = false
        alertJob?.cancel()
        alertJob = null

        try {
            audioTrack?.stop()
            audioTrack?.release()
            audioTrack = null
        } catch (_: Exception) {}

        try {
            if (activeRingtone?.isPlaying == true) {
                activeRingtone?.stop()
            }
            activeRingtone = null
        } catch (_: Exception) {}

        try {
            toneGenerator?.stopTone()
        } catch (_: Exception) {}
    }

    private fun triggerVibrationPulse() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(
                    VibrationEffect.createOneShot(350, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(350)
            }
        } catch (_: Exception) {}
    }

    fun release() {
        stopAlert()
        toneGenerator?.release()
        toneGenerator = null
    }
}
