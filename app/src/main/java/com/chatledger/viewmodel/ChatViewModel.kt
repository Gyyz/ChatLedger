package com.chatledger.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.chatledger.ai.model.AiConfig
import com.chatledger.ai.model.AiResponse
import com.chatledger.ai.provider.AiProviderFactory
import com.chatledger.data.database.AppDatabase
import com.chatledger.data.entity.*
import com.chatledger.data.repository.ChatRepository
import com.chatledger.data.repository.ExpenseRepository
import com.chatledger.util.ImageUtils
import com.chatledger.util.SettingsManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val chatRepo = ChatRepository(db.chatMessageDao())
    private val expenseRepo = ExpenseRepository(db.expenseDao())
    private val settingsManager = SettingsManager(application)

    val messages: Flow<List<ChatMessage>> = chatRepo.getAllMessages()
    val aiConfig: StateFlow<AiConfig> = settingsManager.aiConfig
        .stateIn(viewModelScope, SharingStarted.Eagerly, AiConfig())

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init {
        // 发送欢迎消息（如果是首次）
        viewModelScope.launch {
            chatRepo.getAllMessages().first().let { msgs ->
                if (msgs.isEmpty()) {
                    chatRepo.insert(
                        ChatMessage(
                            content = "👋 你好！我是 ChatLedger 记账助手。\n\n" +
                                    "你可以通过以下方式记账：\n" +
                                    "• 直接告诉我消费信息，如「午饭花了35」\n" +
                                    "• 拍照识别收据 📷\n" +
                                    "• 发送支付截图 🖼️\n" +
                                    "• 语音输入 🎤\n\n" +
                                    "开始记账吧！",
                            type = MessageType.ASSISTANT
                        )
                    )
                }
            }
        }
    }

    /**
     * 发送文字消息
     */
    fun sendTextMessage(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            // 保存用户消息
            chatRepo.insert(
                ChatMessage(content = text, type = MessageType.USER_TEXT)
            )

            processWithAi(text)
        }
    }

    /**
     * 发送图片（收据/截图）
     */
    fun sendImage(uri: Uri) {
        viewModelScope.launch {
            val context = getApplication<Application>()
            chatRepo.insert(
                ChatMessage(
                    content = "📷 发送了一张图片",
                    type = MessageType.USER_IMAGE,
                    imageUri = uri.toString()
                )
            )

            val imageData = ImageUtils.uriToBase64(context, uri)
            if (imageData == null) {
                addAssistantMessage("❌ 无法读取图片，请重试")
                return@launch
            }

            processImageWithAi(imageData.first, imageData.second)
        }
    }

    /**
     * 发送语音转文字结果
     */
    fun sendVoiceText(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            chatRepo.insert(
                ChatMessage(content = "🎤 $text", type = MessageType.USER_VOICE)
            )
            processWithAi(text)
        }
    }

    private suspend fun processWithAi(text: String) {
        val config = aiConfig.value
        if (config.apiKey.isBlank()) {
            addAssistantMessage("⚠️ 请先在设置中配置 AI API Key")
            return
        }

        _isLoading.value = true
        _error.value = null

        try {
            val provider = AiProviderFactory.create(config.provider)
            val response = provider.parseTextInput(text, config)
            handleAiResponse(response)
        } catch (e: Exception) {
            addAssistantMessage("❌ 处理失败: ${e.message}")
        } finally {
            _isLoading.value = false
        }
    }

    private suspend fun processImageWithAi(base64: String, mimeType: String) {
        val config = aiConfig.value
        if (config.apiKey.isBlank()) {
            addAssistantMessage("⚠️ 请先在设置中配置 AI API Key")
            return
        }

        _isLoading.value = true
        try {
            val provider = AiProviderFactory.create(config.provider)
            val response = provider.parseImageInput(base64, mimeType, config = config)
            handleAiResponse(response)
        } catch (e: Exception) {
            addAssistantMessage("❌ 图片处理失败: ${e.message}")
        } finally {
            _isLoading.value = false
        }
    }

    private suspend fun handleAiResponse(response: AiResponse) {
        when (response) {
            is AiResponse.ExpensesParsed -> {
                // 保存支出记录
                for (parsed in response.expenses) {
                    expenseRepo.insert(
                        Expense(
                            amount = parsed.amount,
                            category = parsed.category,
                            description = parsed.description,
                            merchant = parsed.merchant,
                            isIncome = parsed.isIncome,
                            source = "ai"
                        )
                    )
                }
                addAssistantMessage(response.summary)
            }
            is AiResponse.ChatReply -> {
                addAssistantMessage(response.message)
            }
            is AiResponse.Error -> {
                addAssistantMessage("❌ ${response.message}")
            }
        }
    }

    /**
     * 手动添加支出
     */
    fun addManualExpense(
        amount: Double,
        category: ExpenseCategory,
        description: String,
        isIncome: Boolean = false
    ) {
        viewModelScope.launch {
            val expense = Expense(
                amount = amount,
                category = category,
                description = description,
                isIncome = isIncome,
                source = "manual"
            )
            expenseRepo.insert(expense)

            val emoji = if (isIncome) "💰" else category.emoji
            val typeText = if (isIncome) "收入" else "支出"
            addAssistantMessage(
                "$emoji 已记录${typeText}：$description ¥${"%.2f".format(amount)} [${category.displayName}]"
            )
        }
    }

    private suspend fun addAssistantMessage(content: String) {
        chatRepo.insert(
            ChatMessage(content = content, type = MessageType.ASSISTANT)
        )
    }

    fun clearError() {
        _error.value = null
    }
}
