package com.chatledger.ai.provider

import com.chatledger.ai.model.AiConfig
import com.chatledger.ai.model.AiResponse
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * Google Gemini Provider
 */
class GeminiProvider : AiProviderInterface {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    override suspend fun parseTextInput(text: String, config: AiConfig): AiResponse {
        return callApi(
            config = config,
            userText = text,
            imageBase64 = null,
            mimeType = null
        )
    }

    override suspend fun parseImageInput(
        imageBase64: String,
        mimeType: String,
        additionalText: String?,
        config: AiConfig
    ): AiResponse {
        return callApi(
            config = config,
            userText = additionalText ?: ExpensePrompts.IMAGE_PROMPT,
            imageBase64 = imageBase64,
            mimeType = mimeType
        )
    }

    override suspend fun chat(message: String, config: AiConfig): AiResponse {
        return callApi(config, message, null, null)
    }

    private suspend fun callApi(
        config: AiConfig,
        userText: String,
        imageBase64: String?,
        mimeType: String?
    ): AiResponse = withContext(Dispatchers.IO) {
        try {
            val partsArray = JsonArray().apply {
                // System instruction as first text part
                add(JsonObject().apply {
                    addProperty("text", ExpensePrompts.SYSTEM_PROMPT + "\n\n" + userText)
                })
                // Image if present
                if (imageBase64 != null && mimeType != null) {
                    add(JsonObject().apply {
                        add("inline_data", JsonObject().apply {
                            addProperty("mime_type", mimeType)
                            addProperty("data", imageBase64)
                        })
                    })
                }
            }

            val body = JsonObject().apply {
                add("contents", JsonArray().apply {
                    add(JsonObject().apply {
                        add("parts", partsArray)
                    })
                })
            }

            val model = config.getEffectiveModel()
            val baseUrl = config.getEffectiveBaseUrl()
            val url = "$baseUrl/v1beta/models/$model:generateContent?key=${config.apiKey}"

            val request = Request.Builder()
                .url(url)
                .addHeader("Content-Type", "application/json")
                .post(body.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext AiResponse.Error("API错误 (${response.code}): $responseBody")
            }

            val json = gson.fromJson(responseBody, JsonObject::class.java)
            val text = json.getAsJsonArray("candidates")
                ?.get(0)?.asJsonObject
                ?.getAsJsonObject("content")
                ?.getAsJsonArray("parts")
                ?.get(0)?.asJsonObject
                ?.get("text")?.asString ?: ""

            ResponseParser.parse(text)
        } catch (e: Exception) {
            AiResponse.Error("请求失败: ${e.message}")
        }
    }
}
