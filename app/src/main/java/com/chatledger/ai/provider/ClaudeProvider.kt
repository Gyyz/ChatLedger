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

class ClaudeProvider : AiProviderInterface {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    override suspend fun parseTextInput(text: String, config: AiConfig): AiResponse {
        return callApi(
            config = config,
            messages = listOf(
                mapOf(
                    "role" to "user",
                    "content" to text
                )
            )
        )
    }

    override suspend fun parseImageInput(
        imageBase64: String,
        mimeType: String,
        additionalText: String?,
        config: AiConfig
    ): AiResponse {
        val contentArray = JsonArray().apply {
            add(JsonObject().apply {
                addProperty("type", "image")
                add("source", JsonObject().apply {
                    addProperty("type", "base64")
                    addProperty("media_type", mimeType)
                    addProperty("data", imageBase64)
                })
            })
            add(JsonObject().apply {
                addProperty("type", "text")
                addProperty("text", additionalText ?: ExpensePrompts.IMAGE_PROMPT)
            })
        }

        val messagesArray = JsonArray().apply {
            add(JsonObject().apply {
                addProperty("role", "user")
                add("content", contentArray)
            })
        }

        return callApiRaw(config, messagesArray)
    }

    override suspend fun chat(message: String, config: AiConfig): AiResponse {
        return callApi(
            config = config,
            messages = listOf(mapOf("role" to "user", "content" to message))
        )
    }

    private suspend fun callApi(
        config: AiConfig,
        messages: List<Map<String, Any>>
    ): AiResponse {
        val messagesArray = JsonArray()
        for (msg in messages) {
            messagesArray.add(JsonObject().apply {
                addProperty("role", msg["role"] as String)
                addProperty("content", msg["content"] as String)
            })
        }
        return callApiRaw(config, messagesArray)
    }

    private suspend fun callApiRaw(
        config: AiConfig,
        messagesArray: JsonArray
    ): AiResponse = withContext(Dispatchers.IO) {
        try {
            val body = JsonObject().apply {
                addProperty("model", config.getEffectiveModel())
                addProperty("max_tokens", config.maxTokens)
                addProperty("system", ExpensePrompts.SYSTEM_PROMPT)
                add("messages", messagesArray)
            }

            val request = Request.Builder()
                .url("${config.getEffectiveBaseUrl()}/v1/messages")
                .addHeader("x-api-key", config.apiKey)
                .addHeader("anthropic-version", "2023-06-01")
                .addHeader("content-type", "application/json")
                .post(body.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext AiResponse.Error("API错误 (${response.code}): $responseBody")
            }

            val json = gson.fromJson(responseBody, JsonObject::class.java)
            val text = json.getAsJsonArray("content")
                ?.get(0)?.asJsonObject
                ?.get("text")?.asString ?: ""

            ResponseParser.parse(text)
        } catch (e: Exception) {
            AiResponse.Error("请求失败: ${e.message}")
        }
    }
}
