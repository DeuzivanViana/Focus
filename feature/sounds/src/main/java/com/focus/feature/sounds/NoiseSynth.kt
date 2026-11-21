package com.focus.feature.sounds

import com.focus.core.model.FocusSound
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

/**
 * Generates the focus sounds sample by sample. Nothing is recorded or downloaded:
 * every sound is a filtered random signal, so there are no audio files, no licenses
 * to track and nothing added to the app size. Pure Kotlin, so it is unit-tested.
 *
 * Output is mono floats in -1..1 at [sampleRate].
 */
class NoiseSynth(private val sound: FocusSound, private val sampleRate: Int = SAMPLE_RATE, seed: Int = 42) {
    private val random = Random(seed)

    // Pink noise (Paul Kellet's economy filter).
    private var p0 = 0f; private var p1 = 0f; private var p2 = 0f

    // Brown noise integrator.
    private var brown = 0f

    // Rain: high-passed hiss plus short filtered drops.
    private var hpPrevIn = 0f; private var hpPrevOut = 0f
    private var dropEnv = 0f; private var dropDecay = 0f; private var dropLp = 0f; private var dropCut = 0.5f
    private val dropChance = 38f / sampleRate

    // Ocean: slow swells.
    private var phase = 0.0
    private var swellLp = 0f

    private fun white(): Float = random.nextFloat() * 2f - 1f

    private fun pink(): Float {
        val w = white()
        p0 = 0.99765f * p0 + w * 0.0990460f
        p1 = 0.96300f * p1 + w * 0.2965164f
        p2 = 0.57000f * p2 + w * 1.0526913f
        return (p0 + p1 + p2 + w * 0.1848f) * 0.10f
    }

    private fun brown(): Float {
        brown = (brown + 0.02f * white()) / 1.02f
        return brown * 3.5f
    }

    private fun rain(): Float {
        // Hiss: pink noise without the low rumble (one-pole high-pass).
        val x = pink()
        val hp = 0.97f * (hpPrevOut + x - hpPrevIn)
        hpPrevIn = x; hpPrevOut = hp
        // Drops: random short bursts, each with its own brightness.
        if (dropEnv < 0.01f && random.nextFloat() < dropChance) {
            dropEnv = 0.25f + random.nextFloat() * 0.75f
            dropDecay = exp(-1f / (sampleRate * (0.004f + random.nextFloat() * 0.01f)))
            dropCut = 0.15f + random.nextFloat() * 0.6f
        }
        dropLp += dropCut * (white() * dropEnv - dropLp)
        dropEnv *= dropDecay
        return hp * 1.1f + dropLp * 0.9f
    }

    private fun ocean(): Float {
        // Two slow sines make the swell irregular (about 9 s and 13 s).
        phase += 1.0 / sampleRate
        val swell = 0.55 + 0.30 * sin(2 * PI * phase / 9.0) + 0.15 * sin(2 * PI * phase / 13.0 + 1.3)
        swellLp += 0.0005f * (swell.toFloat() - swellLp)
        val level = swellLp.coerceIn(0.1f, 1f)
        return brown() * level * 0.9f + pink() * level * level * 0.7f
    }

    fun next(): Float = when (sound) {
        FocusSound.NONE -> 0f
        FocusSound.WHITE -> white() * 0.35f
        FocusSound.PINK -> pink()
        FocusSound.BROWN -> brown()
        FocusSound.RAIN -> rain()
        FocusSound.OCEAN -> ocean()
    }.coerceIn(-1f, 1f)

    /** Fills [out] with 16-bit PCM. */
    fun fill(out: ShortArray, gain: Float = 1f) {
        for (i in out.indices) out[i] = (next() * gain * Short.MAX_VALUE).toInt().toShort()
    }

    companion object {
        const val SAMPLE_RATE = 44_100
    }
}
