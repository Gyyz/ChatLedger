package com.chatledger.ai.provider

import com.chatledger.ai.model.AiConfig
import com.chatledger.ai.model.AiResponse

/**
 * AI 供应商统一接口
 */
interface AiProviderInterface {

    /**
     * 解析文字输入为支出记录
     */
    suspend fun parseTextInput(text: String, config: AiConfig): AiResponse

    /**
     * 解析图片（收据/截图）为支出记录
     */
    suspend fun parseImageInput(
        imageBase64: String,
        mimeType: String,
        additionalText: String? = null,
        config: AiConfig
    ): AiResponse

    /**
     * 通用对话（非记账相关的问题）
     */
    suspend fun chat(message: String, config: AiConfig): AiResponse
}

/**
 * 系统提示词 — 所有供应商共享
 */
object ExpensePrompts {

    val SYSTEM_PROMPT = """
你是 ChatLedger 的智能记账助手。你的任务是帮助用户记录支出和收入。

## 核心规则
1. 当用户提到消费/花费/支出/买了/付了等相关内容时，提取金额、类别、描述
2. 如果用户输入的是收据照片或截图，仔细分析图片内容提取所有消费项
3. 回复要简洁友好，用中文

## 支出类别（必须使用以下之一）
FOOD(餐饮), TRANSPORT(交通), SHOPPING(购物), ENTERTAINMENT(娱乐),
HOUSING(住房), MEDICAL(医疗), EDUCATION(教育), UTILITIES(水电),
COMMUNICATION(通讯), CLOTHING(服饰), TRAVEL(旅行), GIFT(礼物),
INVESTMENT(投资), INCOME(收入), OTHER(其他)

## 响应格式
当识别到支出/收入时，在回复末尾添加 JSON 块：
```json
[{"amount": 金额, "category": "类别", "description": "描述", "merchant": "商家或null", "isIncome": false}]
```

如果没有识别到任何消费信息，只需正常对话即可，不要添加 JSON。

## 示例
用户: "今天星巴克喝了杯拿铁 38块"
回复: "已记录！星巴克拿铁 ¥38.00 ☕
```json
[{"amount": 38.0, "category": "FOOD", "description": "星巴克拿铁", "merchant": "星巴克", "isIncome": false}]
```"

用户: "打车去公司花了25"
回复: "打车费已记录 🚕
```json
[{"amount": 25.0, "category": "TRANSPORT", "description": "打车去公司", "merchant": null, "isIncome": false}]
```"

用户: "工资到账8000"
回复: "收入已记录 💰
```json
[{"amount": 8000.0, "category": "INCOME", "description": "工资", "merchant": null, "isIncome": true}]
```"
""".trimIndent()

    val IMAGE_PROMPT = """
请仔细分析这张图片（可能是收据、账单、支付截图等），提取出所有的消费/支出信息。
对于每一笔消费，识别：金额、类别、描述、商家名称。
如果图片不清楚或没有消费信息，请说明。
""".trimIndent()
}
