package com.example.core.audio

import android.annotation.SuppressLint
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.sqrt

/**
 * Robust PCM16 bi-directional audio pipeline handling:
 * - AudioRecord microphone capture at 16kHz PCM16 Mono
 * - Wake-word stream routing
 * - Real-time amplitude calculation for dynamic visual waveforms
 * - AudioTrack low-latency streaming playback
 * - Real-time Barge-in / interruption detection
 */
class AudioPipeline(
    private val wakeWordDetector: WakeWordDetector,
    private val onWakeWordDetected: () -> Unit,
    private val onBargeInDetected: () -> Unit,
    private val onPcmChunkCaptured: (ByteArray) -> Unit
) {
    companion object {
        private const val TAG = "KovaAudioPipeline"
        const val SAMPLE_RATE = 16000
        const val CHANNEL_CONFIG_IN = AudioFormat.CHANNEL_IN_MONO
        const val CHANNEL_CONFIG_OUT = AudioFormat.CHANNEL_OUT_MONO
        const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        private const val BUFFER_FRAMES = 1024
    }

    private var audioRecord: AudioRecord? = null
    private var audioTrack: AudioTrack? = null

    private var recordingJob: Job? = null
    private var playbackJob: Job? = null

    private val audioPlaybackQueue = ConcurrentLinkedQueue<ByteArray>()

    private val _inputAmplitude = MutableStateFlow(0f)
    val inputAmplitude: StateFlow<Float> = _inputAmplitude.asStateFlow()

    private val _outputAmplitude = MutableStateFlow(0f)
    val outputAmplitude: StateFlow<Float> = _outputAmplitude.asStateFlow()

    @Volatile
    private var isRecording = false

    @Volatile
    private var isPlaying = false

    @Volatile
    private var isKovaSpeaking = false

    @Volatile
    private var isWakeWordListeningEnabled = true

    fun setWakeWordListeningEnabled(enabled: Boolean) {
        isWakeWordListeningEnabled = enabled
    }

    fun setKovaSpeaking(speaking: Boolean) {
        isKovaSpeaking = speaking
        if (!speaking) {
            _outputAmplitude.value = 0f
        }
    }

    @SuppressLint("MissingPermission")
    fun startRecording(coroutineScope: CoroutineScope): Boolean {
        if (isRecording) return true

        try {
            val minBufferSize = AudioRecord.getMinBufferSize(
                SAMPLE_RATE,
                CHANNEL_CONFIG_IN,
                AUDIO_FORMAT
            ).coerceAtLeast(BUFFER_FRAMES * 2)

            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG_IN,
                AUDIO_FORMAT,
                minBufferSize * 2
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "AudioRecord failed to initialize")
                return false
            }

            audioRecord?.startRecording()
            isRecording = true

            recordingJob = coroutineScope.launch(Dispatchers.IO) {
                val shortBuffer = ShortArray(BUFFER_FRAMES)
                val byteBuffer = ByteArray(BUFFER_FRAMES * 2)

                while (isActive && isRecording) {
                    val readSamples = audioRecord?.read(shortBuffer, 0, BUFFER_FRAMES) ?: -1
                    if (readSamples > 0) {
                        // Calculate RMS amplitude
                        var sumSquares = 0.0
                        for (i in 0 until readSamples) {
                            val sample = shortBuffer[i].toDouble()
                            sumSquares += sample * sample

                            // Convert to byte buffer (little endian)
                            val shortVal = shortBuffer[i].toInt()
                            byteBuffer[i * 2] = (shortVal and 0x00FF).toByte()
                            byteBuffer[i * 2 + 1] = ((shortVal shr 8) and 0x00FF).toByte()
                        }

                        val rms = sqrt(sumSquares / readSamples).toFloat()
                        val normalized = (rms / 32768f).coerceIn(0f, 1f)
                        _inputAmplitude.value = normalized

                        // 1. Barge-in / Interruption check while Kova is speaking
                        if (isKovaSpeaking && normalized > 0.06f) {
                            Log.d(TAG, "Barge-in speech detected during playback (amp=$normalized)")
                            stopPlayback()
                            onBargeInDetected()
                            continue
                        }

                        // 2. Local Wake Word Detection when active
                        if (isWakeWordListeningEnabled && !isKovaSpeaking) {
                            val detected = wakeWordDetector.processSample(shortBuffer, readSamples)
                            if (detected) {
                                Log.i(TAG, "Wake word 'Kova' triggered by local detector!")
                                onWakeWordDetected()
                            }
                        }

                        // 3. Dispatch raw PCM chunk to Gemini live stream if active
                        onPcmChunkCaptured(byteBuffer.copyOf(readSamples * 2))
                    }
                }
            }
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error starting AudioRecord: ${e.message}", e)
            isRecording = false
            return false
        }
    }

    fun stopRecording() {
        isRecording = false
        recordingJob?.cancel()
        recordingJob = null
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping AudioRecord: ${e.message}")
        }
        audioRecord = null
        _inputAmplitude.value = 0f
    }

    fun initAudioTrack(): Boolean {
        if (audioTrack != null) return true
        try {
            val minBufferSize = AudioTrack.getMinBufferSize(
                SAMPLE_RATE,
                CHANNEL_CONFIG_OUT,
                AUDIO_FORMAT
            ).coerceAtLeast(BUFFER_FRAMES * 4)

            val attributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANT)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()

            val format = AudioFormat.Builder()
                .setSampleRate(SAMPLE_RATE)
                .setChannelMask(CHANNEL_CONFIG_OUT)
                .setEncoding(AUDIO_FORMAT)
                .build()

            audioTrack = AudioTrack(
                attributes,
                format,
                minBufferSize,
                AudioTrack.MODE_STREAM,
                android.media.AudioManager.AUDIO_SESSION_ID_GENERATE
            )

            if (audioTrack?.state == AudioTrack.STATE_INITIALIZED) {
                audioTrack?.play()
                return true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to init AudioTrack: ${e.message}")
        }
        return false
    }

    fun playPcmChunk(pcmData: ByteArray) {
        audioPlaybackQueue.offer(pcmData)
        if (!isPlaying) {
            startPlaybackWorker()
        }
    }

    private fun startPlaybackWorker() {
        if (audioTrack == null && !initAudioTrack()) return
        isPlaying = true
        isKovaSpeaking = true

        playbackJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                while (isActive && isPlaying) {
                    val chunk = audioPlaybackQueue.poll()
                    if (chunk != null && chunk.isNotEmpty()) {
                        audioTrack?.write(chunk, 0, chunk.size)

                        // Calculate output amplitude for speaking waveform
                        var sum = 0.0
                        val sampleCount = chunk.size / 2
                        for (i in 0 until sampleCount) {
                            val low = chunk[i * 2].toInt() and 0xFF
                            val high = chunk[i * 2 + 1].toInt()
                            val sample = (high shl 8) or low
                            sum += sample * sample
                        }
                        val rms = sqrt(sum / sampleCount.coerceAtLeast(1)).toFloat()
                        _outputAmplitude.value = (rms / 32768f).coerceIn(0f, 1f)
                    } else {
                        // Empty queue
                        if (audioPlaybackQueue.isEmpty()) {
                            _outputAmplitude.value = 0f
                            kotlinx.coroutines.delay(50)
                            if (audioPlaybackQueue.isEmpty()) {
                                break
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Playback worker error: ${e.message}")
            } finally {
                isPlaying = false
                isKovaSpeaking = false
                _outputAmplitude.value = 0f
            }
        }
    }

    fun stopPlayback() {
        isPlaying = false
        isKovaSpeaking = false
        audioPlaybackQueue.clear()
        playbackJob?.cancel()
        playbackJob = null
        try {
            audioTrack?.pause()
            audioTrack?.flush()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping AudioTrack: ${e.message}")
        }
        _outputAmplitude.value = 0f
    }

    fun release() {
        stopRecording()
        stopPlayback()
        try {
            audioTrack?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing AudioTrack: ${e.message}")
        }
        audioTrack = null
    }
}
