package com.voxlyra.audio

import android.Manifest
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.util.Base64
import androidx.core.content.PermissionChecker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File

/**
 * Gerencia a gravação de áudio no dispositivo.
 * Grava em formato WAV para compatibilidade com o Gemini.
 */
class AudioRecorder(private val context: Context) {

    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null

    /**
     * Verifica se tem permissão de gravação.
     */
    fun hasPermission(): Boolean {
        return PermissionChecker.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PermissionChecker.PERMISSION_GRANTED
    }

    /**
     * Inicia a gravação de áudio.
     * Salva em um arquivo WAV temporário.
     */
    fun startRecording(): Boolean {
        if (!hasPermission()) return false

        try {
            outputFile = File(context.cacheDir, "voxlyra_recording_${System.currentTimeMillis()}.wav")
            
            recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                MediaRecorder()
            }

            recorder?.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                setAudioSamplingRate(8000)
                setAudioEncodingBitRate(12200)
                setOutputFile(outputFile?.absolutePath)
                prepare()
                start()
            }
            return true
        } catch (e: Exception) {
            recorder = null
            outputFile = null
            return false
        }
    }

    /**
     * Para a gravação e retorna o áudio em Base64.
     */
    fun stopRecording(): AudioResult? {
        try {
            recorder?.apply {
                stop()
                release()
            }
            recorder = null

            val file = outputFile ?: return null
            if (!file.exists()) return null

            val audioBytes = file.readBytes()
            val base64 = Base64.encodeToString(audioBytes, Base64.NO_WRAP)
            val durationMs = getAudioDuration(file)

            // Limpa o arquivo temporário
            file.delete()

            return AudioResult(
                base64 = base64,
                mimeType = "audio/3gpp",
                durationMs = durationMs
            )
        } catch (e: Exception) {
            recorder?.release()
            recorder = null
            return null
        }
    }

    /**
     * Cancela a gravação sem processar.
     */
    fun cancelRecording() {
        try {
            recorder?.apply {
                stop()
                release()
            }
            recorder = null
            outputFile?.delete()
            outputFile = null
        } catch (e: Exception) {
            // Silêncio
        }
    }

    private fun getAudioDuration(file: File): Long {
        // Estimativa aproximada: arquivo 3GPP AMR ~ 1 segundo = ~1.5KB
        return (file.length() * 1000 / 1500).coerceAtMost(30000)
    }
}

data class AudioResult(
    val base64: String,
    val mimeType: String,
    val durationMs: Long
)