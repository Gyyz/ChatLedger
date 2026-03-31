package com.chatledger.ai.provider

import com.chatledger.ai.model.AiProvider

/**
 * 根据配置创建对应的 AI Provider
 */
object AiProviderFactory {

    fun create(provider: AiProvider): AiProviderInterface {
        return when (provider) {
            AiProvider.CLAUDE -> ClaudeProvider()
            AiProvider.OPENAI -> OpenAiCompatibleProvider()
            AiProvider.DEEPSEEK -> OpenAiCompatibleProvider()
            AiProvider.GEMINI -> GeminiProvider()
            AiProvider.CUSTOM -> OpenAiCompatibleProvider()
        }
    }
}
