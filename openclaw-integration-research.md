# ChatLedger x OpenClaw 集成方案调研报告

## OpenClaw 是什么？

OpenClaw（原名 Clawdbot/Moltbot）是 2025 年末发布的开源 AI Agent 工具，由奥地利开发者 Peter Steinberger 创建。它在 2026 年 2 月即突破了 10 万 GitHub Stars，是目前增长最快的开源项目之一。

**核心能力：** OpenClaw 通过消息平台（Telegram、WhatsApp、Slack、Discord、微信、飞书等 30+ 渠道）作为用户界面，连接 LLM 来自主执行任务——浏览网页、读写文件、运行命令、调用 API。

## 集成方案概览

OpenClaw 提供三种扩展机制，我们可以用来接入 ChatLedger：

| 方案 | 适合场景 | 复杂度 | 推荐度 |
|------|---------|--------|--------|
| **Skill（技能）** | 最简单，用 SKILL.md 定义指令 | 低 | ⭐⭐⭐⭐⭐ |
| **Gateway API** | 外部系统主动调用 OpenClaw | 中 | ⭐⭐⭐ |
| **Webhook** | 外部事件触发 OpenClaw 动作 | 中 | ⭐⭐⭐ |

---

## 方案一：OpenClaw Skill（推荐）

### 原理
创建一个 `chatledger` 技能目录，包含 SKILL.md 文件。当用户在 OpenClaw 中说"记账"相关的话，Agent 自动加载该技能，通过 HTTP 调用 ChatLedger 的 API 来记录支出。

### 目录结构
```
~/.openclaw/skills/chatledger/
├── SKILL.md          # 技能定义和指令
└── scripts/
    └── add_expense.py  # 可选：辅助脚本
```

### SKILL.md 示例
```markdown
---
name: chatledger
description: 记账助手 - 通过自然语言记录日常支出，支持中文。说"记账"、"花了"、"买了"等触发。
metadata: {"openclaw":{"requires":{"env":["CHATLEDGER_URL"]},"emoji":"💰"}}
---

# ChatLedger 记账技能

当用户提到记账、花费、支出、购买等关键词时，使用此技能。

## 工作流程
1. 从用户消息中提取：金额、类别、描述、日期
2. 调用 ChatLedger API 记录支出
3. 返回确认信息给用户

## API 调用
curl -X POST "$CHATLEDGER_URL/api/expense" \
  -H "Content-Type: application/json" \
  -d '{"amount": 金额, "category": "类别", "description": "描述", "date": "日期"}'

## 类别映射
餐饮🍜 | 交通🚗 | 购物🛍️ | 娱乐🎮 | 医疗💊 | 教育📚 |
居住🏠 | 通讯📱 | 服饰👔 | 美容💄 | 社交👥 | 宠物🐾 |
旅行✈️ | 数码💻 | 其他📦
```

### 需要什么
ChatLedger 需要暴露一个 REST API 端点。我们需要给 chatledger-web.html 加一个轻量后端，或者做成本地服务。

---

## 方案二：ChatLedger 作为本地 HTTP 服务

### 架构
```
用户 → OpenClaw (任意消息平台)
         ↓ Agent 识别记账意图
       curl → http://localhost:3210/api/expense
         ↓
       ChatLedger 本地服务 (Node.js/Python)
         ↓
       SQLite / localStorage 持久化
```

### 实现思路
将 ChatLedger 改造为一个本地运行的 Web 服务：
- 保留现有的 HTML 界面（浏览器打开使用）
- 新增 REST API 层（供 OpenClaw 调用）
- 使用 SQLite 替代 localStorage（支持多进程访问）

### API 设计
```
POST /api/expense          # 添加支出
GET  /api/expenses         # 查询支出列表
GET  /api/stats/weekly     # 周统计
GET  /api/stats/monthly    # 月统计
GET  /api/categories       # 类别列表
```

---

## 方案三：纯前端 URL Scheme / Deep Link

### 原理
不需要后端，通过 URL 参数直接打开 ChatLedger 并自动填入数据：

```
chatledger://add?amount=35&category=餐饮&desc=午餐
```

或者基于 Web 版本：
```
http://localhost:8080/chatledger-web.html#add=35&cat=餐饮&desc=午餐
```

ChatLedger 读取 URL hash 参数，自动记录。

### 优点
- 不需要后端服务
- 实现简单
- OpenClaw 只需打开 URL

### 缺点
- 需要浏览器环境
- 无法返回确认信息给 OpenClaw

---

## 推荐实施路径

### 第一步：给 ChatLedger 添加 REST API（Node.js 版本）
创建一个轻量 Express/Hono 服务，整合现有的本地智能匹配逻辑和 AI 解析能力。

### 第二步：创建 OpenClaw Skill
编写 SKILL.md，让 OpenClaw Agent 能通过自然语言调用 ChatLedger API。

### 第三步：发布到 ClawHub
将 skill 发布到 OpenClaw 官方技能注册表 (clawhub.com)，让更多用户使用。

### 效果预览
```
用户 (在微信/Telegram 等): "午饭吃了碗牛肉面 15块"
OpenClaw Agent: "好的，已记录：🍜 餐饮 - 牛肉面 ¥15.00 ✅"

用户: "这周花了多少钱？"
OpenClaw Agent: "本周支出统计：
  🍜 餐饮: ¥280
  🚗 交通: ¥156
  🛍️ 购物: ¥89
  总计: ¥525"
```

---

## 参考资源
- OpenClaw 官方文档: https://docs.openclaw.ai/tools/skills
- OpenClaw GitHub: https://github.com/openclaw/openclaw
- ClawHub 技能注册表: https://github.com/openclaw/clawhub
- OpenClaw API 集成指南: https://lumadock.com/tutorials/openclaw-custom-api-integration-guide
