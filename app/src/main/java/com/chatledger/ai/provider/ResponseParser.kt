package com.chatledger.ai.provider

import com.chatledger.ai.model.AiResponse
import com.chatledger.ai.model.ParsedExpense
import com.chatledger.data.entity.ExpenseCategory
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.reflect.TypeToken

/**
 * 统一解析 AI 响应，从文本中提取 JSON 格式的支出数据
 */
object ResponseParser {

    private val gson = Gson()
    private val jsonBlockRegex = Regex("```json\\s*\\n?(.*?)\\n?```", RegexOption.DOT_MATCHES_ALL)

    fun parse(responseText: String): AiResponse {
        val match = jsonBlockRegex.find(responseText)

        if (match != null) {
            try {
                val jsonStr = match.groupValues[1].trim()
                val listType = object : TypeToken<List<Map<String, Any>>>() {}.type
                val items: List<Map<String, Any>> = gson.fromJson(jsonStr, listType)

                val expenses = items.map { item ->
                    ParsedExpense(
                        amount = (item["amount"] as? Number)?.toDouble() ?: 0.0,
                        category = ExpenseCategory.fromName(
                            item["category"] as? String ?: "OTHER"
                        ),
                        description = item["description"] as? String ?: "",
                        merchant = item["merchant"] as? String,
                        isIncome = item["isIncome"] as? Boolean ?: false
                    )
                }

                // 提取 JSON 之前的文字作为摘要
                val summary = responseText
                    .substring(0, match.range.first)
                    .trim()
                    .ifEmpty { "已记录 ${expenses.size} 笔交易" }

                return AiResponse.ExpensesParsed(expenses, summary)
            } catch (e: Exception) {
                // JSON 解析失败，当作普通对话
            }
        }

        return AiResponse.ChatReply(responseText)
    }
}
