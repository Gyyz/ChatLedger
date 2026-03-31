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
 * OpenAI 兼容的 Provider — 适用于 OpenAI、DeepSeek、以及任何 OpenAI API 兼容的服务
 */
class OpenAiCompatibleProvider : AiProviderInterface {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    override suspend fun parseTextInput(text: String, config: AiConfig): AiResponse {
        val messages = JsonArray().apply {
            add(JsonObject().apply {
                addProperty("role", "system")
                addProperty("content", ExpensePrompts.SYSTEM_PROMPT)
            })
            add(JsonObject().apply {
                addProperty("role", "user")
                addProperty("content", text)
            })
        }
        return callApi(config, messages)
    }

    override suspend fun parseImageInput(
        imageBase64: String,
        mimeType: String,
        additionalText: String?,
        config: AiConfig
    ): AiResponse {
        val contentArray = JsonArray().apply {
            add(JsonObject().apply {
                addProperty("type", "image_url")
                add("image_url", JsonObject().apply {
                    addProperty("url", "data:$mimeType;base64,$imageBase64")
                })
            })
            add(JsonObject().apply {
                addProperty("type", "text")
                addProperty("text", additionalText ?: ExpensePrompts.IMAGE_PROMPT)
            })
        }

        val messages = JsonArray().apply {
            add(JsonObject().apply {
                addProperty("role", "system")
                addProperty("content", ExpensePrompts.SYSTEM_PROMPT)
            })
            add(JsonObject().apply {
                addProperty("role", "user")
                add("content", contentArray)
            })
        }
        return callApi(config, messages)
    }

    override suspend fun chat(message: String, config: AiConfig): AiResponse {
        val messages = JsonArray().apply {
            add(JsonObject().apply {
                addProperty("role", "system")
                addProperty("content", ExpensePrompts.SYSTEM_PROMPT)
            })
            add(JsonObject().apply {
                addProperty("role", "user")
                addProperty("content", message)
            })
        }
        return callApi(config, messages)
    }

    private suspend fun callApi(
        config: AiConfig,
        messages: JsonArray
    ): AiResponse = withContext(Dispatchers.IO) {
        try {
            val body = JsonObject().apply {
                addProperty("model", config.getEffectiveModel())
                addProperty("max_tokens", config.maxTokens)
                add("messages", messages)
            }

            val baseUrl = config.getEffectiveBaseUrl()
            val url = "$baseUrl/v1/chat/completions"

            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer ${config.apiKey}")
                .addHeader("Content-Type", "application/json")
                .post(body.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext AiResponse.Error("API错误 (${response.code}): $responseBody")
            }

            val json = gson.fromJson(responseBody, JsonObject::class.java)
            val text = json.getAsJsonArray("choices")
                ?.get(0)?.asJsonObject
                ?.getAsJsonObject("message")
                ?.get("content")?.asString ?: ""

            ResponseParser.parse(text)
        } catch (e: Exception) {
            AiResponse.Error("请求失败: ${e.message}")
        }
    }
}
