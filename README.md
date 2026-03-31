# 📒 ChatLedger — 聊天式智能记账

ChatLedger 是一款聊天式智能记账应用。像和朋友聊天一样说出你的消费，它会自动识别金额、分类、商家，帮你记下来。

支持文字、语音、拍照收据、截图识别四种输入方式，内置 200+ 中文关键词的本地智能匹配引擎，无需 API Key 即可自动分类 15 种支出类别。配置 AI 后还可解锁收据/截图 OCR 识别。

---

## 功能一览

**输入方式**

- 💬 自然语言文字输入（"午饭花了35"、"星巴克拿铁38元"）
- 🎤 语音识别输入（基于 Web Speech API）
- 📷 拍照识别收据（需配置 AI）
- 🖼️ 截图识别支付记录（需配置 AI）
- ✏️ 手动录入（选择类别、填写金额）

**智能识别**

- 🧠 本地 NLP 引擎：200+ 中文消费关键词，覆盖餐饮、交通、购物等 15 个类别
- 🏪 商家自动检测：星巴克、肯德基、滴滴、美团等 28 个知名商家
- 💰 金额提取：支持"35块"、"¥38"、"花了25元"等 7 种中文金额表达
- 🤖 多 AI 引擎：Claude / OpenAI / Gemini / DeepSeek / 自定义 OpenAI 兼容

**数据浏览**

- 📋 账单页：按月浏览全部记录，按日分组，支持前后月翻页，年度总览
- 📊 统计页：饼图（支出占比）、柱状图（分类金额）、每日趋势、分类明细
- 🗑️ 删除记录：账单页和统计页每条记录右侧 × 按钮，确认弹窗防误删

**外部集成**

- 🦞 OpenClaw 技能：在微信、Telegram、Slack 等 30+ 消息平台通过自然语言记账
- 📡 REST API：供任意外部系统调用

---

## 快速开始

### 方式一：纯前端版（零依赖）

直接在浏览器中打开 `chatledger-web.html` 即可使用，数据保存在 localStorage。

```bash
open chatledger-web.html
# 或
python3 -m http.server 8080  # 然后访问 http://localhost:8080/chatledger-web.html
```

### 方式二：后端服务版（支持 API 接入）

带 REST API 的完整版本，使用 SQLite 持久化，支持 OpenClaw 等外部系统接入。

```bash
# 1. 安装依赖
cd server && npm install

# 2. 启动
node server.js
# 或使用启动脚本
cd .. && ./start.sh
```

启动后：
- Web UI：http://localhost:3210
- API：http://localhost:3210/api

可选环境变量：

| 变量 | 说明 | 默认值 |
|------|------|--------|
| `CHATLEDGER_PORT` | 服务端口 | `3210` |
| `CHATLEDGER_DB` | SQLite 数据库路径 | `server/chatledger.db` |
| `CHATLEDGER_TOKEN` | API 认证令牌（留空则不鉴权） | 空 |

---

## API 文档

### POST /api/record — 自然语言记账

核心 API，传入自然语言文本，自动解析并记录。

```bash
curl -X POST http://localhost:3210/api/record \
  -H "Content-Type: application/json" \
  -d '{"text": "午饭花了35块", "source": "openclaw"}'
```

```json
{
  "success": true,
  "message": "🍜 已记录支出：午饭 ¥35.00 [餐饮]",
  "expense": {
    "id": 1,
    "amount": 35,
    "category": "FOOD",
    "categoryName": "餐饮",
    "description": "午饭",
    "merchant": null,
    "isIncome": false,
    "confidence": 0.8
  }
}
```

### POST /api/expense — 结构化记账

```bash
curl -X POST http://localhost:3210/api/expense \
  -H "Content-Type: application/json" \
  -d '{"amount": 38, "category": "FOOD", "description": "星巴克拿铁", "merchant": "星巴克"}'
```

### GET /api/expenses — 查询记录

```bash
curl "http://localhost:3210/api/expenses?limit=20&offset=0"
curl "http://localhost:3210/api/expenses?start=2026-03-01&end=2026-03-31"
```

### GET /api/stats — 统计数据

```bash
curl "http://localhost:3210/api/stats?period=week"   # 本周
curl "http://localhost:3210/api/stats?period=month"   # 本月
curl "http://localhost:3210/api/stats?period=year"    # 今年
```

### GET /api/bills — 月度账单

```bash
curl "http://localhost:3210/api/bills?year=2026&month=3"  # 某月明细
curl "http://localhost:3210/api/bills?year=2026"           # 年度各月汇总
```

### DELETE /api/expense/:id — 删除记录

```bash
curl -X DELETE http://localhost:3210/api/expense/1
```

### GET /api/categories — 类别列表

### GET /api/health — 健康检查

---

## OpenClaw 集成

ChatLedger 提供 OpenClaw 技能，安装后可在微信、Telegram、Slack 等消息平台直接记账。

### 安装

```bash
# 安装技能到 OpenClaw
cd openclaw-skill && ./install.sh

# 设置环境变量
export CHATLEDGER_URL=http://localhost:3210
```

### 使用

在任意 OpenClaw 消息平台中：

```
你：午饭吃了碗牛肉面 15块
🤖：🍜 已记录支出：牛肉面 ¥15.00 [餐饮] ✅

你：这周花了多少钱？
🤖：📊 本周支出统计
    💸 总支出: ¥525.00
    🍜 餐饮: ¥280  🚗 交通: ¥156  🛍️ 购物: ¥89
```

---

## 支持的类别

| Emoji | 类别 | 关键词示例 |
|-------|------|-----------|
| 🍜 | 餐饮 | 午饭、外卖、星巴克、奶茶、火锅 |
| 🚗 | 交通 | 打车、地铁、滴滴、加油、高铁 |
| 🛍️ | 购物 | 淘宝、京东、超市、日用品 |
| 🎮 | 娱乐 | 电影、游戏、会员、KTV |
| 🏠 | 住房 | 房租、物业费、房贷、酒店 |
| 💊 | 医疗 | 看病、药店、体检、牙科 |
| 📚 | 教育 | 网课、书、培训、学费 |
| 💡 | 水电 | 电费、水费、燃气、宽带 |
| 📱 | 通讯 | 话费、流量、手机费 |
| 👔 | 服饰 | 衣服、鞋、化妆品、理发 |
| ✈️ | 旅行 | 旅游、签证、机票预订 |
| 🎁 | 礼物 | 红包、份子钱、生日礼物 |
| 📈 | 投资 | 基金、股票、保险 |
| 💰 | 收入 | 工资、奖金、退款、报销 |
| 📦 | 其他 | 未匹配的消费 |

---

## 项目结构

```
ChatLedger/
├── chatledger-web.html        # 完整单文件 Web 应用（可独立使用）
├── start.sh                   # 一键启动脚本
├── server/
│   ├── server.js              # Express + SQLite 后端服务
│   ├── package.json
│   └── chatledger.db          # SQLite 数据库（运行后生成）
├── openclaw-skill/
│   ├── SKILL.md               # OpenClaw 技能定义
│   └── install.sh             # 技能安装脚本
└── app/                       # Android 原生版（Kotlin + Jetpack Compose）
```

---

## 技术栈

**前端：** HTML / CSS / JavaScript（单文件，零依赖）、SVG 图表、Web Speech API

**后端：** Node.js、Express、better-sqlite3

**AI 集成：** Claude API、OpenAI API、Gemini API、DeepSeek API、OpenAI 兼容接口

**外部集成：** OpenClaw Skill（支持 30+ 消息平台）

---

## License

MIT
