package com.focus.feature.sounds

import com.focus.core.model.FocusSound
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.sqrt

class NoiseSynthTest {
    private fun rms(sound: FocusSound): Double {
        val synth = NoiseSynth(sound)
        val n = NoiseSynth.SAMPLE_RATE * 10
        var sum = 0.0
        var mean = 0.0
        repeat(n) {
            val v = synth.next().toDouble()
            assertTrue("$sound out of range: $v", abs(v) <= 1.0)
            sum += v * v
            mean += v
        }
        assertTrue("$sound has a DC offset", abs(mean / n) < 0.08)
        return sqrt(sum / n)
    }

    @Test
    fun everySoundIsAudibleButNotClipping() {
        FocusSound.entries.filter { it != FocusSound.NONE }.forEach { s ->
            val r = rms(s)
            assertTrue("$s too quiet: $r", r > 0.04)
            assertTrue("$s too loud: $r", r < 0.45)
        }
    }

    @Test
    fun noneIsSilent() {
        val synth = NoiseSynth(FocusSound.NONE)
        repeat(1000) { assertTrue(synth.next() == 0f) }
    }
}
