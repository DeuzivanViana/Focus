package com.focus.feature.sounds

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.focus.core.data.repository.SettingsRepository
import com.focus.core.model.FocusSound
import com.focus.core.model.TimerPhase
import com.focus.core.model.TimerStatus
import com.focus.core.timer.TimerController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Plays the chosen focus sound while a focus phase is running (paused or break = silence),
 * or a preview while the picker is open. Audio is synthesized on the fly by [NoiseSynth]
 * and streamed through one [AudioTrack]; it fades in and out instead of cutting.
 * It does not request audio focus, so it mixes with the user's own music if they want both.
 */
@Singleton
class FocusSoundPlayer @Inject constructor(
    private val timer: TimerController,
    private val settings: SettingsRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val preview = MutableStateFlow<FocusSound?>(null)
    private var started = false

    @Volatile private var volume = 0.6f

    fun start() {
        if (started) return
        started = true
        scope.launch {
            val focusing = timer.state.map { it.status == TimerStatus.RUNNING && it.phase == TimerPhase.FOCUS }.distinctUntilChanged()
            val chosen = settings.settings.map { it.focusSound to it.soundVolume }.distinctUntilChanged()
            combine(focusing, chosen, preview) { active, (sound, vol), pre ->
                volume = vol / 100f
                pre ?: sound.takeIf { active }
            }
                .distinctUntilChanged()
                .collectLatest { sound -> if (sound != null && sound != FocusSound.NONE) play(sound) }
        }
    }

    /** Plays [sound] right away (picker preview); null returns to the normal rule. */
    fun preview(sound: FocusSound?) {
        preview.value = sound
    }

    fun setPreviewVolume(percent: Int) {
        volume = percent.coerceIn(0, 100) / 100f
    }

    /** Streams until the coroutine is cancelled (sound changed or focus ended), then fades out. */
    private suspend fun play(sound: FocusSound) = withContext(Dispatchers.IO) {
        val rate = NoiseSynth.SAMPLE_RATE
        val minBuf = AudioTrack.getMinBufferSize(rate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
        val chunk = ShortArray(CHUNK_FRAMES)
        val track = runCatching {
            AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_UNKNOWN)
                        .build(),
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(rate)
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build(),
                )
                .setBufferSizeInBytes(maxOf(minBuf, CHUNK_FRAMES * 2 * 2))
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
        }.getOrNull() ?: return@withContext
        val synth = NoiseSynth(sound, rate, seed = System.nanoTime().toInt())
        var gain = 0f
        try {
            track.play()
            while (currentCoroutineContext().isActive) {
                // Ease towards the target volume: a soft fade-in and smooth volume changes.
                val target = volume
                gain += (target - gain) * FADE_STEP
                synth.fill(chunk, gain)
                track.write(chunk, 0, chunk.size)
            }
        } finally {
            // Fade out over a few chunks, even though the coroutine was cancelled.
            repeat(FADE_OUT_CHUNKS) { i ->
                synth.fill(chunk, gain * (1f - (i + 1f) / FADE_OUT_CHUNKS))
                runCatching { track.write(chunk, 0, chunk.size) }
            }
            runCatching { track.stop() }
            track.release()
        }
    }

    private companion object {
        /** ~46 ms per chunk at 44.1 kHz. */
        const val CHUNK_FRAMES = 2048
        const val FADE_STEP = 0.08f
        const val FADE_OUT_CHUNKS = 10
    }
}
