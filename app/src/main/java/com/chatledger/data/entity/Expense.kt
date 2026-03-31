package com.chatledger.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 支出类别枚举
 */
enum class ExpenseCategory(val displayName: String, val emoji: String) {
    FOOD("餐饮", "🍜"),
    TRANSPORT("交通", "🚗"),
    SHOPPING("购物", "🛍️"),
    ENTERTAINMENT("娱乐", "🎮"),
    HOUSING("住房", "🏠"),
    MEDICAL("医疗", "💊"),
    EDUCATION("教育", "📚"),
    UTILITIES("水电", "💡"),
    COMMUNICATION("通讯", "📱"),
    CLOTHING("服饰", "👔"),
    TRAVEL("旅行", "✈️"),
    GIFT("礼物", "🎁"),
    INVESTMENT("投资", "📈"),
    INCOME("收入", "💰"),
    OTHER("其他", "📋");

    companion object {
        fun fromName(name: String): ExpenseCategory {
            return entries.find {
                it.displayName == name || it.name.equals(name, ignoreCase = true)
            } ?: OTHER
        }
    }
}

/**
 * 消息类型
 */
enum class MessageType {
    USER_TEXT,      // 用户手动文字输入
    USER_IMAGE,     // 用户发送的图片（收据/截图）
    USER_VOICE,     // 用户语音输入
    ASSISTANT,      // AI 助手回复
    SYSTEM          // 系统消息（如统计提醒）
}

/**
 * 聊天消息实体
 */
@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val content: String,
    val type: MessageType,
    val imageUri: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val relatedExpenseId: Long? = null
)

/**
 * 支出记录实体
 */
@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val amount: Double,
    val category: ExpenseCategory,
    val description: String,
    val merchant: String? = null,
    val isIncome: Boolean = false,
    val source: String = "manual",  // manual, receipt, screenshot, voice
    val imageUri: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val note: String? = null
)
