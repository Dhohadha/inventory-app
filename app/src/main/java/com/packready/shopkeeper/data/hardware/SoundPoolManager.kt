package com.packready.shopkeeper.data.hardware

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.SoundPool
import android.os.Build
import com.packready.shopkeeper.R
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sin

/**
 * High-performance, sub-10ms latency audio feedback system.
 * Generates crisp physical Kirana chimes:
 * 1. Barcode Scan: 2,200 Hz laser chirp (80ms)
 * 2. Sale Success: Dual-tone cash register bell (520 Hz / 880 Hz harmonic, 400ms)
 * 3. Error Buzz: 220 Hz low warning buzz (150ms)
 */
@Singleton
class SoundPoolManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val soundPool: SoundPool
    private var scanChimeId: Int = 0
    private var saleBellId: Int = 0
    private var errorBuzzId: Int = 0
    private var isInitialized = false

    private val audioScope = CoroutineScope(Dispatchers.Default)

    init {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(6)
            .setAudioAttributes(audioAttributes)
            .build()

        soundPool.setOnLoadCompleteListener { _, _, status ->
            if (status == 0) {
                isInitialized = true
            }
        }

        initializeTones()
    }

    private fun initializeTones() {
        audioScope.launch {
            try {
                // Generate high-quality PCM WAV files dynamically in app cache for SoundPool
                val scanFile = createToneWavFile("scan_chime.wav", 2200.0, 80, ToneShape.CHIRP)
                val saleFile = createDualToneWavFile("sale_bell.wav", 520.0, 880.0, 400)
                val errorFile = createToneWavFile("error_buzz.wav", 220.0, 150, ToneShape.SQUARE)

                scanChimeId = soundPool.load(scanFile.absolutePath, 1)
                saleBellId = soundPool.load(saleFile.absolutePath, 1)
                errorBuzzId = soundPool.load(errorFile.absolutePath, 1)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun playScanSuccess() {
        if (scanChimeId != 0) {
            soundPool.play(scanChimeId, 1.0f, 1.0f, 1, 0, 1.0f)
        } else {
            // Immediate fallback synthesized tone
            playSynthTone(2200.0, 80)
        }
    }

    fun playSaleSuccess() {
        if (saleBellId != 0) {
            soundPool.play(saleBellId, 1.0f, 1.0f, 2, 0, 1.0f)
        } else {
            playSynthDualTone(520.0, 880.0, 350)
        }
    }

    fun playErrorBuzz() {
        if (errorBuzzId != 0) {
            soundPool.play(errorBuzzId, 1.0f, 1.0f, 3, 0, 1.0f)
        } else {
            playSynthTone(220.0, 150)
        }
    }

    private enum class ToneShape { SINE, CHIRP, SQUARE }

    private fun createToneWavFile(fileName: String, freq: Double, durationMs: Int, shape: ToneShape): File {
        val file = File(context.cacheDir, fileName)
        val sampleRate = 44100
        val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
        val pcmData = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val currentFreq = if (shape == ToneShape.CHIRP) {
                freq + (i.toDouble() / numSamples) * 600.0 // sweep upward
            } else freq

            val envelope = 1.0 - (i.toDouble() / numSamples) // decay envelope
            val sample = when (shape) {
                ToneShape.SQUARE -> if (sin(2.0 * Math.PI * currentFreq * t) > 0) 0.6 else -0.6
                else -> sin(2.0 * Math.PI * currentFreq * t)
            }
            pcmData[i] = (sample * envelope * Short.MAX_VALUE * 0.8).toInt().toShort()
        }

        writeWavFile(file, pcmData, sampleRate)
        return file
    }

    private fun createDualToneWavFile(fileName: String, freq1: Double, freq2: Double, durationMs: Int): File {
        val file = File(context.cacheDir, fileName)
        val sampleRate = 44100
        val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
        val pcmData = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val envelope = Math.exp(-4.0 * (i.toDouble() / numSamples)) // Bell exponential decay
            val sample = (sin(2.0 * Math.PI * freq1 * t) * 0.6) + (sin(2.0 * Math.PI * freq2 * t) * 0.4)
            pcmData[i] = (sample * envelope * Short.MAX_VALUE * 0.85).toInt().toShort()
        }

        writeWavFile(file, pcmData, sampleRate)
        return file
    }

    private fun writeWavFile(file: File, pcmData: ShortArray, sampleRate: Int) {
        val byteData = ByteArray(pcmData.size * 2)
        for (i in pcmData.indices) {
            byteData[i * 2] = (pcmData[i].toInt() and 0x00FF).toByte()
            byteData[i * 2 + 1] = ((pcmData[i].toInt() shr 8) and 0x00FF).toByte()
        }

        val totalDataLen = byteData.size + 36
        val totalAudioLen = byteData.size
        val channels = 1
        val byteRate = sampleRate * 2

        FileOutputStream(file).use { out ->
            val header = ByteArray(44)
            header[0] = 'R'.code.toByte(); header[1] = 'I'.code.toByte(); header[2] = 'F'.code.toByte(); header[3] = 'F'.code.toByte()
            header[4] = (totalDataLen and 0xff).toByte()
            header[5] = ((totalDataLen shr 8) and 0xff).toByte()
            header[6] = ((totalDataLen shr 16) and 0xff).toByte()
            header[7] = ((totalDataLen shr 24) and 0xff).toByte()
            header[8] = 'W'.code.toByte(); header[9] = 'A'.code.toByte(); header[10] = 'V'.code.toByte(); header[11] = 'E'.code.toByte()
            header[12] = 'f'.code.toByte(); header[13] = 'm'.code.toByte(); header[14] = 't'.code.toByte(); header[15] = ' '.code.toByte()
            header[16] = 16; header[17] = 0; header[18] = 0; header[19] = 0 // PCM format size
            header[20] = 1; header[21] = 0 // format = 1 (PCM)
            header[22] = channels.toByte(); header[23] = 0
            header[24] = (sampleRate and 0xff).toByte()
            header[25] = ((sampleRate shr 8) and 0xff).toByte()
            header[26] = ((sampleRate shr 16) and 0xff).toByte()
            header[27] = ((sampleRate shr 24) and 0xff).toByte()
            header[28] = (byteRate and 0xff).toByte()
            header[29] = ((byteRate shr 8) and 0xff).toByte()
            header[30] = ((byteRate shr 16) and 0xff).toByte()
            header[31] = ((byteRate shr 24) and 0xff).toByte()
            header[32] = 2; header[33] = 0 // block align
            header[34] = 16; header[35] = 0 // bits per sample
            header[36] = 'd'.code.toByte(); header[37] = 'a'.code.toByte(); header[38] = 't'.code.toByte(); header[39] = 'a'.code.toByte()
            header[40] = (totalAudioLen and 0xff).toByte()
            header[41] = ((totalAudioLen shr 8) and 0xff).toByte()
            header[42] = ((totalAudioLen shr 16) and 0xff).toByte()
            header[43] = ((totalAudioLen shr 24) and 0xff).toByte()

            out.write(header, 0, 44)
            out.write(byteData)
        }
    }

    private fun playSynthTone(freq: Double, durationMs: Int) {
        audioScope.launch {
            try {
                val sampleRate = 44100
                val count = (sampleRate * (durationMs / 1000.0)).toInt()
                val samples = ShortArray(count)
                for (i in 0 until count) {
                    val t = i.toDouble() / sampleRate
                    samples[i] = (sin(2.0 * Math.PI * freq * t) * Short.MAX_VALUE * 0.7).toInt().toShort()
                }
                val track = AudioTrack.Builder()
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
                track.write(samples, 0, samples.size)
                track.play()
            } catch (e: Exception) {
                // Ignore fallback failures
            }
        }
    }

    private fun playSynthDualTone(freq1: Double, freq2: Double, durationMs: Int) {
        audioScope.launch {
            try {
                val sampleRate = 44100
                val count = (sampleRate * (durationMs / 1000.0)).toInt()
                val samples = ShortArray(count)
                for (i in 0 until count) {
                    val t = i.toDouble() / sampleRate
                    val s = (sin(2.0 * Math.PI * freq1 * t) * 0.5) + (sin(2.0 * Math.PI * freq2 * t) * 0.5)
                    samples[i] = (s * Short.MAX_VALUE * 0.8).toInt().toShort()
                }
                val track = AudioTrack.Builder()
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
                track.write(samples, 0, samples.size)
                track.play()
            } catch (e: Exception) {
                // Ignore fallback failures
            }
        }
    }
}
