package com.voxlyra.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Cliente para chamar a API do Google Gemini.
 * Usa a Gemini 2.0 Flash para respostas rápidas.
 */
class GeminiClient(private val apiKey: String) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val baseUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent"

    /**
     * Envia uma mensagem de texto para o Gemini e retorna a resposta.
     */
    suspend fun sendMessage(text: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val requestBody = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", text)
                            })
                        })
                    })
                })
                // Configurações de segurança mais permissivas para um assistente pessoal
                put("safetySettings", JSONArray().apply {
                    for (category in listOf(
                        "HARM_CATEGORY_HARASSMENT",
                        "HARM_CATEGORY_HATE_SPEECH",
                        "HARM_CATEGORY_SEXUALLY_EXPLICIT",
                        "HARM_CATEGORY_DANGEROUS_CONTENT"
                    )) {
                        put(JSONObject().apply {
                            put("category", category)
                            put("threshold", "BLOCK_NONE")
                        })
                    }
                })
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.7)
                    put("maxOutputTokens", 2048)
                    put("topK", 40)
                    put("topP", 0.95)
                })
            }

            val response = client.newCall(
                Request.Builder()
                    .url("$baseUrl?key=$apiKey")
                    .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
                    .addHeader("Content-Type", "application/json")
                    .build()
            ).execute()

            val responseBody = response.body?.string()
            if (!response.isSuccessful) {
                return@withContext Result.failure(
                    Exception("Erro na API: ${response.code} - $responseBody")
                )
            }

            val jsonResponse = JSONObject(responseBody)
            val text = jsonResponse
                .getJSONArray("candidates")
                .getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text")

            Result.success(text)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Envia texto junto com áudio codificado em base64 para o Gemini.
     * O Gemini 2.0 Flash suporta entrada multimodal.
     */
    suspend fun sendAudioMessage(audioBase64: String, mimeType: String = "audio/wav"): Result<String> = withContext(Dispatchers.IO) {
        try {
            val requestBody = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            // Instrução de sistema
                            put(JSONObject().apply {
                                put("text", "Você é a VoxLyra, uma assistente de voz amigável e prestativa. " +
                                        "O usuário enviou um áudio. Transcreva o que ele disse e responda de forma " +
                                        "natural e útil em português brasileiro. Seja concisa e direta.")
                            })
                            // Áudio
                            put(JSONObject().apply {
                                put("inlineData", JSONObject().apply {
                                    put("mimeType", mimeType)
                                    put("data", audioBase64)
                                })
                            })
                        })
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.7)
                    put("maxOutputTokens", 1024)
                })
            }

            val response = client.newCall(
                Request.Builder()
                    .url("$baseUrl?key=$apiKey")
                    .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
                    .addHeader("Content-Type", "application/json")
                    .build()
            ).execute()

            val responseBody = response.body?.string()
            if (!response.isSuccessful) {
                return@withContext Result.failure(
                    Exception("Erro na API: ${response.code} - $responseBody")
                )
            }

            val jsonResponse = JSONObject(responseBody)
            val text = jsonResponse
                .getJSONArray("candidates")
                .getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text")

            Result.success(text)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}