package com.example.data.remote

import com.example.data.security.KeystoreSecretVault
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

data class GeminiTurn(
    val role: String, // "user" or "model"
    val text: String,
    val inlineMimeType: String? = null,
    val inlineBase64: String? = null
)

data class GeminiAttachment(
    val label: String,
    val mimeType: String,
    val base64Data: String
)

class GeminiRestClient(
    private val vault: KeystoreSecretVault
) {
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(45, TimeUnit.SECONDS)
        .build()

    suspend fun generateContent(
        model: String,
        systemInstruction: String,
        history: List<GeminiTurn>,
        prompt: String,
        attachment: GeminiAttachment? = null,
        temperature: Double = 0.7
    ): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = vault.getGeminiApiKey()?.trim()
        if (apiKey.isNullOrBlank()) {
            return@withContext Result.failure(
                IllegalStateException("Gemini API key not configured. Add your key in Settings -> API Keys.")
            )
        }

        val payload = JSONObject().apply {
            if (systemInstruction.isNotBlank()) {
                put(
                    "system_instruction",
                    JSONObject().put(
                        "parts",
                        JSONArray().put(JSONObject().put("text", systemInstruction))
                    )
                )
            }
            val contents = JSONArray()
            history.takeLast(14).forEach { turn ->
                val parts = JSONArray().apply {
                    if (turn.text.isNotBlank()) put(JSONObject().put("text", turn.text))
                    if (!turn.inlineMimeType.isNullOrBlank() && !turn.inlineBase64.isNullOrBlank()) {
                        put(
                            JSONObject().put(
                                "inline_data",
                                JSONObject()
                                    .put("mime_type", turn.inlineMimeType)
                                    .put("data", turn.inlineBase64)
                            )
                        )
                    }
                }
                contents.put(
                    JSONObject()
                        .put("role", if (turn.role == "model" || turn.role == "assistant") "model" else "user")
                        .put("parts", parts)
                )
            }
            val currentParts = JSONArray().apply {
                put(JSONObject().put("text", prompt))
                if (attachment != null) {
                    put(
                        JSONObject().put(
                            "inline_data",
                            JSONObject()
                                .put("mime_type", attachment.mimeType)
                                .put("data", attachment.base64Data)
                        )
                    )
                }
            }
            contents.put(
                JSONObject()
                    .put("role", "user")
                    .put("parts", currentParts)
            )
            put("contents", contents)
            put(
                "generationConfig",
                JSONObject().put("temperature", temperature)
            )
        }

        val cleanModel = model.ifBlank { DEFAULT_MODEL }
        val request = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/$cleanModel:generateContent")
            .addHeader("Content-Type", "application/json")
            .addHeader("x-goog-api-key", apiKey)
            .post(payload.toString().toRequestBody(JSON_MEDIA))
            .build()

        executeCancellable(request)
    }

    suspend fun verifyApiKey(apiKeyCandidate: String, model: String = DEFAULT_MODEL): Result<String> =
        withContext(Dispatchers.IO) {
            val cleanKey = apiKeyCandidate.trim()
            if (cleanKey.isEmpty()) {
                return@withContext Result.failure(IllegalArgumentException("API key cannot be empty."))
            }
            val payload = JSONObject().apply {
                put(
                    "contents",
                    JSONArray().put(
                        JSONObject()
                            .put("role", "user")
                            .put("parts", JSONArray().put(JSONObject().put("text", "Reply with OK.")))
                    )
                )
            }
            val request = Request.Builder()
                .url("https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent")
                .addHeader("Content-Type", "application/json")
                .addHeader("x-goog-api-key", cleanKey)
                .post(payload.toString().toRequestBody(JSON_MEDIA))
                .build()
            executeCancellable(request)
        }

    private suspend fun executeCancellable(request: Request): Result<String> =
        suspendCancellableCoroutine { cont ->
            val call = client.newCall(request)
            cont.invokeOnCancellation { call.cancel() }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    if (cont.isActive) {
                        val msg = if (call.isCanceled()) "Generation stopped." else (e.localizedMessage ?: "Network failure")
                        cont.resume(Result.failure(IOException(msg, e)))
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    response.use { res ->
                        val bodyStr = res.body?.string().orEmpty()
                        if (!res.isSuccessful) {
                            val apiMessage = runCatching {
                                JSONObject(bodyStr).optJSONObject("error")?.optString("message")
                            }.getOrNull()
                            val msg = apiMessage?.takeIf { it.isNotBlank() }
                                ?: "Gemini API error (${res.code})"
                            if (cont.isActive) cont.resume(Result.failure(IllegalStateException(msg)))
                            return
                        }
                        val parsedText = extractCandidateText(bodyStr)
                        if (parsedText.isBlank()) {
                            if (cont.isActive) {
                                cont.resume(Result.failure(IllegalStateException("Gemini returned an empty response.")))
                            }
                        } else {
                            if (cont.isActive) cont.resume(Result.success(parsedText))
                        }
                    }
                }
            })
        }

    private fun extractCandidateText(rawJson: String): String {
        return try {
            val root = JSONObject(rawJson)
            val candidates = root.optJSONArray("candidates") ?: return ""
            if (candidates.length() == 0) return ""
            val content = candidates.getJSONObject(0).optJSONObject("content") ?: return ""
            val parts = content.optJSONArray("parts") ?: return ""
            buildString {
                for (i in 0 until parts.length()) {
                    val partText = parts.getJSONObject(i).optString("text")
                    if (partText.isNotEmpty()) {
                        if (isNotEmpty()) append("\n")
                        append(partText)
                    }
                }
            }.trim()
        } catch (_: Exception) {
            ""
        }
    }

    companion object {
        const val DEFAULT_MODEL = "gemini-2.5-flash"
        val SUPPORTED_MODELS = listOf(
            "gemini-2.5-flash",
            "gemini-2.5-pro",
            "gemini-2.0-flash"
        )
        private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()
    }
}
