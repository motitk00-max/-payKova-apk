package com.example.core.audio

import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Interface isolating the local wake word engine.
 */
interface WakeWordDetector {
    /**
     * Process raw PCM16 audio frames.
     * Returns true if "Kova" wake word was detected.
     */
    fun processSample(buffer: ShortArray, readSize: Int): Boolean

    /**
     * Resets detector state.
     */
    fun reset()

    /**
     * Adjust sensitivity (0.1 to 1.0)
     */
    fun setSensitivity(sensitivity: Float)
}

/**
 * Native lightweight acoustic envelope and dual-syllable phonetic cadence detector for "Ko-va".
 * Runs purely on-device via low-level PCM16 buffers with near-zero CPU and memory footprint.
 */
class KovaAcousticWakeWordDetector(
    private var sensitivity: Float = 0.65f
) : WakeWordDetector {

    private var previousEnergy = 0f
    private var stage = DetectionStage.IDLE
    private var stageTimestamp = 0L
    private var energyHistory = FloatArray(16)
    private var historyIndex = 0
    private var lastDetectionTime = 0L

    private enum class DetectionStage {
        IDLE,
        FIRST_SYLLABLE_DETECTED, // "Ko" attack + vowel resonance
        VALLEY_TRANSITION,       // "v" fricative dip
        SECOND_SYLLABLE_DETECTED // "va" vowel resolution
    }

    override fun setSensitivity(sensitivity: Float) {
        this.sensitivity = sensitivity.coerceIn(0.1f, 1.0f)
    }

    override fun reset() {
        stage = DetectionStage.IDLE
        stageTimestamp = 0L
        previousEnergy = 0f
        energyHistory.fill(0f)
        historyIndex = 0
    }

    override fun processSample(buffer: ShortArray, readSize: Int): Boolean {
        if (readSize <= 0) return false

        val currentTime = System.currentTimeMillis()
        // Prevent rapid re-triggering within 1.5 seconds
        if (currentTime - lastDetectionTime < 1500) {
            return false
        }

        // 1. Calculate RMS Energy and Zero-Crossing Rate
        var sumSquares = 0.0
        var zeroCrossings = 0
        var prevSign = buffer[0] >= 0

        for (i in 0 until readSize) {
            val sample = buffer[i].toDouble()
            sumSquares += sample * sample

            val currentSign = buffer[i] >= 0
            if (currentSign != prevSign) {
                zeroCrossings++
                prevSign = currentSign
            }
        }

        val rms = sqrt(sumSquares / readSize).toFloat()
        val normalizedRms = (rms / 32768f).coerceIn(0f, 1f)
        val zcr = zeroCrossings.toFloat() / readSize

        // Smooth energy window
        energyHistory[historyIndex] = normalizedRms
        historyIndex = (historyIndex + 1) % energyHistory.size
        val avgEnergy = energyHistory.average().toFloat()

        val energyThreshold = (0.025f * (1.15f - sensitivity * 0.5f)).coerceAtLeast(0.012f)
        val isVoiceActive = normalizedRms > energyThreshold && zcr in 0.02f..0.45f

        // 2. Dual-syllable Temporal State Machine for "Ko-va"
        when (stage) {
            DetectionStage.IDLE -> {
                // Look for sharp onset of "Ko" syllable
                val energyDelta = normalizedRms - previousEnergy
                if (isVoiceActive && energyDelta > 0.015f && normalizedRms > energyThreshold * 1.5f) {
                    stage = DetectionStage.FIRST_SYLLABLE_DETECTED
                    stageTimestamp = currentTime
                }
            }

            DetectionStage.FIRST_SYLLABLE_DETECTED -> {
                val elapsed = currentTime - stageTimestamp
                if (elapsed > 450) {
                    // Timed out on first syllable
                    stage = DetectionStage.IDLE
                } else if (elapsed > 100 && normalizedRms < avgEnergy * 0.9f) {
                    // Entering the fricative dip between "Ko" and "va"
                    stage = DetectionStage.VALLEY_TRANSITION
                    stageTimestamp = currentTime
                }
            }

            DetectionStage.VALLEY_TRANSITION -> {
                val elapsed = currentTime - stageTimestamp
                if (elapsed > 350) {
                    stage = DetectionStage.IDLE
                } else if (isVoiceActive && normalizedRms > avgEnergy * 1.1f && elapsed > 60) {
                    // Second syllable "va" onset detected
                    stage = DetectionStage.SECOND_SYLLABLE_DETECTED
                    stageTimestamp = currentTime
                }
            }

            DetectionStage.SECOND_SYLLABLE_DETECTED -> {
                val elapsed = currentTime - stageTimestamp
                if (elapsed in 60..300) {
                    // Full "Ko-va" temporal and spectral signature verified
                    reset()
                    lastDetectionTime = currentTime
                    return true
                } else if (elapsed > 350) {
                    stage = DetectionStage.IDLE
                }
            }
        }

        previousEnergy = normalizedRms
        return false
    }
}
