package com.voxlyra.viewmodel

import android.app.Application
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.voxlyra.audio.AudioRecorder
import com.voxlyra.network.GeminiClient
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Estados possíveis do assistente VoxLyra.
 */
enum class VoxLyraState {
    IDLE,           // Pronto pra ouvir
    LISTENING,      // Gravando áudio
    PROCESSING,     // Processando com Gemini
    SPEAKING,       // Falando a resposta
    ERROR           // Deu ruim
}

/**
 * ViewModel principal do VoxLyra.
 * Gerencia estado, gravação, API e TTS.
 */
class VoxLyraViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "VoxLyraVM"
        // ⚠️ IMPORTANTE: Troque esta chave pela sua chave real da API Gemini!
        // Para gerar: https://aistudio.google.com/apikey
        private const val GEMINI_API_KEY = "YOUR_GEMINI_API_KEY_HERE"
    }

    private val audioRecorder = AudioRecorder(application)
    private val geminiClient = GeminiClient(GEMINI_API_KEY)
    private var tts: TextToSpeech? = null
    private var ttsReady = false

    // Estados
    private val _state = MutableStateFlow(VoxLyraState.IDLE)
    val state: StateFlow<VoxLyraState> = _state.asStateFlow()

    private val _transcript = MutableStateFlow("")
    val transcript: StateFlow<String> = _transcript.asStateFlow()

    private val _response = MutableStateFlow("")
    val response: StateFlow<String> = _response.asStateFlow()

    private val _errorMessage = MutableStateFlow("")
    val errorMessage: StateFlow<String> = _errorMessage.asStateFlow()

    private val _counterText = MutableStateFlow("")
    val counterText: StateFlow<String> = _counterText.asStateFlow()

    private var processingJob: Job? = null
    private var startTimeMs: Long = 0L

    init {
        initTTS()
    }

    private fun initTTS() {
        tts = TextToSpeech(getApplication()) { status ->
            ttsReady = status == TextToSpeech.SUCCESS
            tts?.language = Locale("pt", "BR")
            Log.d(TAG, "TTS initialized: $ttsReady")
        }
    }

    /**
     * Ação principal: começar a ouvir.
     */
    fun startListening() {
        if (_state.value == VoxLyraState.PROCESSING) return

        val started = audioRecorder.startRecording()
        if (!started) {
            _errorMessage.value = "Preciso de permissão do microfone para funcionar!"
            _state.value = VoxLyraState.ERROR
            return
        }

        _state.value = VoxLyraState.LISTENING
        startTimeMs = System.currentTimeMillis()
        _transcript.value = ""
        _response.value = ""
        _errorMessage.value = ""

        // Contador visual
        processingJob = viewModelScope.launch {
            while (_state.value == VoxLyraState.LISTENING) {
                val elapsed = (System.currentTimeMillis() - startTimeMs) / 1000
                _counterText.value = "${elapsed}s"
                delay(500)
            }
            _counterText.value = ""
        }
    }

    /**
     * Para a gravação e processa com o Gemini.
     */
    fun stopListening() {
        if (_state.value != VoxLyraState.LISTENING) return

        _state.value = VoxLyraState.PROCESSING
        processingJob?.cancel()

        val audioResult = audioRecorder.stopRecording()
        if (audioResult == null) {
            _errorMessage.value = "Erro ao capturar áudio. Tente novamente."
            _state.value = VoxLyraState.ERROR
            return
        }

        _transcript.value = "Áudio capturado (${audioResult.durationMs / 1000}s)"

        // Envia pro Gemini
        viewModelScope.launch {
            val result = geminiClient.sendAudioMessage(audioResult.base64, audioResult.mimeType)
            result.fold(
                onSuccess = { responseText ->
                    _response.value = responseText
                    _state.value = VoxLyraState.SPEAKING
                    speakResponse(responseText)
                },
                onFailure = { error ->
                    Log.e(TAG, "Gemini API error", error)
                    _errorMessage.value = "Erro ao processar: ${error.message ?: "tente novamente"}"
                    _state.value = VoxLyraState.ERROR
                }
            )
        }
    }

    /**
     * Envia uma mensagem de texto para o Gemini.
     */
    fun sendTextMessage(text: String) {
        if (text.isBlank()) return
        if (_state.value == VoxLyraState.PROCESSING) return

        _state.value = VoxLyraState.PROCESSING
        _transcript.value = text
        _response.value = ""
        _errorMessage.value = ""

        viewModelScope.launch {
            val result = geminiClient.sendMessage(text)
            result.fold(
                onSuccess = { responseText ->
                    _response.value = responseText
                    _state.value = VoxLyraState.SPEAKING
                    speakResponse(responseText)
                },
                onFailure = { error ->
                    Log.e(TAG, "Gemini API error", error)
                    _errorMessage.value = "Erro ao processar: ${error.message ?: "tente novamente"}"
                    _state.value = VoxLyraState.ERROR
                }
            )
        }
    }

    /**
     * Fala a resposta usando TTS.
     */
    private fun speakResponse(text: String) {
        if (!ttsReady || tts == null) {
            // Se TTS não estiver pronto, só mostra o texto
            _state.value = VoxLyraState.IDLE
            return
        }

        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        
        // Volta ao estado IDLE depois de um tempo
        viewModelScope.launch {
            delay(3000)
            _state.value = VoxLyraState.IDLE
        }
    }

    /**
     * Limpa o estado de erro e volta ao IDLE.
     */
    fun clearError() {
        _errorMessage.value = ""
        _state.value = VoxLyraState.IDLE
    }

    /**
     * Reseta o chat.
     */
    fun reset() {
        _state.value = VoxLyraState.IDLE
        _transcript.value = ""
        _response.value = ""
        _errorMessage.value = ""
        _counterText.value = ""
        processingJob?.cancel()
        audioRecorder.cancelRecording()
    }

    override fun onCleared() {
        super.onCleared()
        tts?.stop()
        tts?.shutdown()
        audioRecorder.cancelRecording()
    }
}