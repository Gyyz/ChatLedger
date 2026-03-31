package com.chatledger.ai.model

import com.chatledger.data.entity.ExpenseCategory

/**
 * AI 供应商枚举
 */
enum class AiProvider(val displayName: String) {
    CLAUDE("Claude (Anthropic)"),
    OPENAI("OpenAI"),
    GEMINI("Google Gemini"),
    DEEPSEEK("DeepSeek"),
    CUSTOM("自定义 (OpenAI 兼容)");
}

/**
 * AI 配置
 */
data class AiConfig(
    val provider: AiProvider = AiProvider.CLAUDE,
    val apiKey: String = "",
    val baseUrl: String = "",  // 自定义端点
    val model: String = "",    // 模型名称
    val maxTokens: Int = 1024
) {
    fun getEffectiveBaseUrl(): String = when {
        baseUrl.isNotBlank() -> baseUrl.trimEnd('/')
        else -> when (provider) {
            AiProvider.CLAUDE -> "https://api.anthropic.com"
            AiProvider.OPENAI -> "https://api.openai.com"
            AiProvider.GEMINI -> "https://generativelanguage.googleapis.com"
            AiProvider.DEEPSEEK -> "https://api.deepseek.com"
            AiProvider.CUSTOM -> ""
        }
    }

    fun getEffectiveModel(): String = when {
        model.isNotBlank() -> model
        else -> when (provider) {
            AiProvider.CLAUDE -> "claude-sonnet-4-20250514"
            AiProvider.OPENAI -> "gpt-4o"
            AiProvider.GEMINI -> "gemini-2.0-flash"
            AiProvider.DEEPSEEK -> "deepseek-chat"
            AiProvider.CUSTOM -> "gpt-4o"
        }
    }
}

/**
 * AI 解析出的支出信息
 */
data class ParsedExpense(
    val amount: Double,
    val category: ExpenseCategory,
    val description: String,
    val merchant: String? = null,
    val isIncome: Boolean = false,
    val confidence: Float = 1.0f
)

/**
 * AI 响应
 */
sealed class AiResponse {
    data class ExpensesParsed(
        val expenses: List<ParsedExpense>,
        val summary: String
    ) : AiResponse()

    data class ChatReply(
        val message: String
    ) : AiResponse()

    data class Error(
        val message: String
    ) : AiResponse()
}
