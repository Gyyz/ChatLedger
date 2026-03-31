#!/usr/bin/env node
/**
 * ChatLedger REST API Server
 *
 * 提供 REST API 供 OpenClaw 等外部系统调用记账功能。
 * 同时作为 ChatLedger Web 前端的后端服务。
 *
 * 启动: node server.js
 * 默认端口: 3210
 */

const express = require('express');
const cors = require('cors');
const Database = require('better-sqlite3');
const path = require('path');
const fs = require('fs');

const app = express();
const PORT = process.env.CHATLEDGER_PORT || 3210;
const API_TOKEN = process.env.CHATLEDGER_TOKEN || ''; // 可选鉴权

// ============= 中间件 =============
app.use(cors());
app.use(express.json({ limit: '10mb' }));

// 静态文件 - 服务 Web 前端
app.use(express.static(path.join(__dirname, '..')));

// 可选 Token 鉴权
function authMiddleware(req, res, next) {
  if (!API_TOKEN) return next(); // 未设置 token 则跳过
  const token = req.headers.authorization?.replace('Bearer ', '');
  if (token !== API_TOKEN) {
    return res.status(401).json({ error: '未授权访问', code: 'UNAUTHORIZED' });
  }
  next();
}

// ============= 数据库 =============
const DB_PATH = process.env.CHATLEDGER_DB || path.join(__dirname, 'chatledger.db');
const db = new Database(DB_PATH);

// 开启 WAL 模式提升并发性能
db.pragma('journal_mode = WAL');

// 创建表
db.exec(`
  CREATE TABLE IF NOT EXISTS expenses (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    amount REAL NOT NULL,
    category TEXT NOT NULL DEFAULT 'OTHER',
    description TEXT DEFAULT '',
    merchant TEXT,
    is_income INTEGER DEFAULT 0,
    source TEXT DEFAULT 'api',
    note TEXT,
    created_at DATETIME DEFAULT (datetime('now', 'localtime')),
    updated_at DATETIME DEFAULT (datetime('now', 'localtime'))
  );

  CREATE TABLE IF NOT EXISTS chat_messages (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    role TEXT NOT NULL,
    content TEXT NOT NULL,
    image_data TEXT,
    created_at DATETIME DEFAULT (datetime('now', 'localtime'))
  );

  CREATE INDEX IF NOT EXISTS idx_expenses_category ON expenses(category);
  CREATE INDEX IF NOT EXISTS idx_expenses_created ON expenses(created_at);
  CREATE INDEX IF NOT EXISTS idx_expenses_is_income ON expenses(is_income);
`);

// ============= 本地智能识别引擎 (从前端移植) =============
const CATEGORIES = {
  FOOD: { name: '餐饮', emoji: '🍜', color: '#FF6B6B' },
  TRANSPORT: { name: '交通', emoji: '🚗', color: '#4ECDC4' },
  SHOPPING: { name: '购物', emoji: '🛍️', color: '#FFE66D' },
  ENTERTAINMENT: { name: '娱乐', emoji: '🎮', color: '#A8E6CF' },
  HOUSING: { name: '住房', emoji: '🏠', color: '#95E1D3' },
  MEDICAL: { name: '医疗', emoji: '💊', color: '#F38181' },
  EDUCATION: { name: '教育', emoji: '📚', color: '#AA96DA' },
  UTILITIES: { name: '水电', emoji: '💡', color: '#FCBAD3' },
  COMMUNICATION: { name: '通讯', emoji: '📱', color: '#6C5CE7' },
  CLOTHING: { name: '服饰', emoji: '👔', color: '#FD79A8' },
  TRAVEL: { name: '旅行', emoji: '✈️', color: '#74B9FF' },
  GIFT: { name: '礼物', emoji: '🎁', color: '#E17055' },
  INVESTMENT: { name: '投资', emoji: '📈', color: '#00B894' },
  INCOME: { name: '收入', emoji: '💰', color: '#00C853' },
  OTHER: { name: '其他', emoji: '📦', color: '#B2BEC3' }
};

const LOCAL_RULES = [
  { keywords: ['早餐','午餐','晚餐','早饭','午饭','晚饭','夜宵','外卖','堂食','餐厅','饭店','食堂','小吃','烧烤','火锅','串串','麻辣烫','冒菜','拉面','米线','面条','饺子','包子','馒头','粥','豆浆','奶茶','咖啡','茶','饮料','果汁','可乐','啤酒','酒','蛋糕','甜品','面包','零食','水果','菜','肉','鱼','虾','蟹','吃饭','吃了','喝了','点了','饿了么','美团外卖','肯德基','KFC','麦当劳','星巴克','Starbucks','瑞幸','luckin','海底捞','必胜客','汉堡','披萨','寿司','便当','盒饭','食材','买菜','超市买吃的'], cat: 'FOOD' },
  { keywords: ['打车','出租车','的士','滴滴','曹操','T3','高德打车','Uber','公交','地铁','公交卡','交通卡','一卡通','火车','高铁','动车','飞机','机票','船票','车票','停车','停车费','加油','油费','充电桩','充电费','过路费','高速费','ETC','骑车','共享单车','哈啰','青桔','美团单车','摩拜','车费','路费','通勤'], cat: 'TRANSPORT' },
  { keywords: ['淘宝','天猫','京东','拼多多','闲鱼','唯品会','抖音商城','快手','亚马逊','购物','买了','网购','逛街','商场','超市','便利店','711','全家','罗森','日用品','洗衣液','洗发水','牙膏','纸巾','垃圾袋','电器','数码','手机壳','充电器','耳机','鼠标','键盘','U盘','家具','装饰','厨具'], cat: 'SHOPPING' },
  { keywords: ['电影','影院','KTV','唱歌','游戏','充值','会员','VIP','视频会员','音乐会员','Netflix','B站大会员','爱奇艺','优酷','腾讯视频','演出','话剧','展览','博物馆','游乐园','密室','剧本杀','桌游','网吧','健身','游泳','瑜伽','球赛','门票','景点','乐园','迪士尼','环球影城','娱乐'], cat: 'ENTERTAINMENT' },
  { keywords: ['房租','租金','月租','押金','物业费','物业','房贷','月供','装修','维修','修理','搬家','中介费','住宿','酒店','民宿','宾馆','旅馆','Airbnb'], cat: 'HOUSING' },
  { keywords: ['医院','看病','挂号','门诊','药','药店','药房','体检','检查','化验','手术','牙科','拔牙','补牙','洗牙','眼科','配镜','眼镜','中医','针灸','理疗','保健','维生素','口罩','创可贴','医疗','诊所'], cat: 'MEDICAL' },
  { keywords: ['学费','培训','课程','网课','书','教材','课本','考试','报名','补习','辅导','家教','培训班','兴趣班','学习','知识付费','得到','喜马拉雅','知乎','文具','笔','本子','打印'], cat: 'EDUCATION' },
  { keywords: ['水费','电费','燃气','煤气','天然气','暖气','供暖','电费单','水电','网费','宽带','WiFi'], cat: 'UTILITIES' },
  { keywords: ['话费','流量','手机费','电话费','充话费','移动','联通','电信','月租费','套餐','短信'], cat: 'COMMUNICATION' },
  { keywords: ['衣服','裤子','裙子','鞋','鞋子','帽子','围巾','手套','袜子','内衣','外套','T恤','衬衫','西装','运动鞋','拖鞋','背包','包包','手表','首饰','项链','戒指','耳环','化妆品','护肤品','面膜','口红','香水','洗面奶','防晒','美甲','理发','剪头','染发','烫发','美容'], cat: 'CLOTHING' },
  { keywords: ['旅游','旅行','出差','签证','护照','行李','酒店预订','机票预订','租车','导游','门票预订','旅行团','自由行','景点门票'], cat: 'TRAVEL' },
  { keywords: ['礼物','送礼','红包','份子钱','随礼','贺礼','生日礼物','圣诞礼物','情人节','母亲节','父亲节','结婚','婚礼','满月','请客','请吃饭'], cat: 'GIFT' },
  { keywords: ['投资','理财','基金','股票','债券','定期','存款','保险','保费','年金','黄金','比特币','加密货币','ETF'], cat: 'INVESTMENT' },
  { keywords: ['工资','薪水','奖金','年终奖','提成','佣金','分红','利息','租金收入','兼职','副业','退款','报销','红包收入','中奖','回款','到账','收到','进账','转入'], cat: 'INCOME', isIncome: true }
];

const AMOUNT_PATTERNS = [
  /(\d+(?:\.\d{1,2})?)\s*[元块圆¥￥]/,
  /[¥￥]\s*(\d+(?:\.\d{1,2})?)/,
  /花了\s*(\d+(?:\.\d{1,2})?)/,
  /付了\s*(\d+(?:\.\d{1,2})?)/,
  /(\d+(?:\.\d{1,2})?)\s*(?:块钱|元钱)/,
  /(?:花|付|交|充|买|打了|用了|消费|支出|收到|到账|进账)\s*(?:了)?\s*(\d+(?:\.\d{1,2})?)/,
  /(\d+(?:\.\d{1,2})?)(?:\s*(?:块|元|¥|￥))?/
];

const KNOWN_MERCHANTS = ['星巴克','瑞幸','肯德基','KFC','麦当劳','海底捞','必胜客','喜茶','奈雪','蜜雪冰城','华莱士','沃尔玛','家乐福','盒马','山姆','Costco','优衣库','ZARA','H&M','MUJI','无印良品','名创优品','滴滴','美团','饿了么','淘宝','京东','拼多多'];

function parseExpenseText(text) {
  // 提取金额
  let amount = 0;
  for (const pat of AMOUNT_PATTERNS) {
    const m = text.match(pat);
    if (m) {
      const val = parseFloat(m[1]);
      if (val > 0 && val < 1000000) { amount = val; break; }
    }
  }
  if (amount === 0) return null;

  // 匹配类别
  let bestCat = 'OTHER';
  let bestScore = 0;
  let isIncome = false;
  let matchedKeyword = '';

  for (const rule of LOCAL_RULES) {
    for (const kw of rule.keywords) {
      if (text.includes(kw) && kw.length > bestScore) {
        bestCat = rule.cat;
        bestScore = kw.length;
        isIncome = !!rule.isIncome;
        matchedKeyword = kw;
      }
    }
  }

  // 提取描述
  let description = text
    .replace(/[¥￥]\s*\d+(\.\d+)?/g, '')
    .replace(/\d+(\.\d+)?\s*[元块圆]/g, '')
    .replace(/花了|付了|买了|充了|交了|用了/g, '')
    .trim();
  if (description.length > 30) description = description.substring(0, 30) + '...';
  if (!description) description = CATEGORIES[bestCat]?.name || '消费';

  // 提取商家
  let merchant = null;
  const merchantPatterns = [
    /在(.{2,8}?)(?:花|付|买|吃|喝|消费)/,
    /(.{2,8}?)(?:的|买的|点的)/,
    /(?:去|到)(.{2,8}?)(?:花|付|买|吃)/
  ];
  for (const mp of merchantPatterns) {
    const mm = text.match(mp);
    if (mm && mm[1].length >= 2) { merchant = mm[1].trim(); break; }
  }
  for (const km of KNOWN_MERCHANTS) {
    if (text.includes(km)) { merchant = km; break; }
  }

  return { amount, category: bestCat, description, merchant, isIncome, confidence: bestScore > 0 ? 0.8 : 0.5 };
}

// ============= SQL 预编译语句 =============
const insertExpense = db.prepare(`
  INSERT INTO expenses (amount, category, description, merchant, is_income, source, note)
  VALUES (@amount, @category, @description, @merchant, @is_income, @source, @note)
`);

const getExpenses = db.prepare(`
  SELECT * FROM expenses ORDER BY created_at DESC LIMIT @limit OFFSET @offset
`);

const getExpensesByDateRange = db.prepare(`
  SELECT * FROM expenses
  WHERE created_at >= @start AND created_at <= @end
  ORDER BY created_at DESC
`);

const getCategoryTotals = db.prepare(`
  SELECT category, SUM(amount) as total, COUNT(*) as count
  FROM expenses
  WHERE is_income = 0 AND created_at >= @start AND created_at <= @end
  GROUP BY category ORDER BY total DESC
`);

const getIncomeTotals = db.prepare(`
  SELECT SUM(amount) as total, COUNT(*) as count
  FROM expenses
  WHERE is_income = 1 AND created_at >= @start AND created_at <= @end
`);

const getDailyTotals = db.prepare(`
  SELECT date(created_at) as day, SUM(amount) as total, COUNT(*) as count
  FROM expenses
  WHERE is_income = 0 AND created_at >= @start AND created_at <= @end
  GROUP BY date(created_at) ORDER BY day
`);

const deleteExpense = db.prepare('DELETE FROM expenses WHERE id = @id');

// ============= API 路由 =============

// --- 健康检查 ---
app.get('/api/health', (req, res) => {
  res.json({ status: 'ok', service: 'ChatLedger', version: '1.0.0', timestamp: new Date().toISOString() });
});

// --- 自然语言记账（核心 API，供 OpenClaw 调用） ---
app.post('/api/record', authMiddleware, (req, res) => {
  const { text, source = 'openclaw' } = req.body;
  if (!text) return res.status(400).json({ error: '请提供记账文本', code: 'MISSING_TEXT' });

  const parsed = parseExpenseText(text);
  if (!parsed) {
    return res.status(422).json({
      error: '未能识别金额，请包含具体金额信息',
      code: 'NO_AMOUNT',
      hint: '例如: "午饭花了35", "星巴克咖啡38元", "打车25块"'
    });
  }

  const cat = CATEGORIES[parsed.category] || CATEGORIES.OTHER;
  const result = insertExpense.run({
    amount: parsed.amount,
    category: parsed.category,
    description: parsed.description,
    merchant: parsed.merchant,
    is_income: parsed.isIncome ? 1 : 0,
    source,
    note: text
  });

  const emoji = parsed.isIncome ? '💰' : cat.emoji;
  const typeText = parsed.isIncome ? '收入' : '支出';

  res.json({
    success: true,
    message: `${emoji} 已记录${typeText}：${parsed.description}${parsed.merchant ? '（' + parsed.merchant + '）' : ''} ¥${parsed.amount.toFixed(2)} [${cat.name}]`,
    expense: {
      id: result.lastInsertRowid,
      amount: parsed.amount,
      category: parsed.category,
      categoryName: cat.name,
      categoryEmoji: cat.emoji,
      description: parsed.description,
      merchant: parsed.merchant,
      isIncome: parsed.isIncome,
      confidence: parsed.confidence
    }
  });
});

// --- 直接添加支出（结构化） ---
app.post('/api/expense', authMiddleware, (req, res) => {
  const { amount, category = 'OTHER', description = '', merchant, isIncome = false, note, source = 'api' } = req.body;

  if (!amount || amount <= 0) {
    return res.status(400).json({ error: '请提供有效金额', code: 'INVALID_AMOUNT' });
  }
  if (!CATEGORIES[category]) {
    return res.status(400).json({ error: '无效类别', code: 'INVALID_CATEGORY', validCategories: Object.keys(CATEGORIES) });
  }

  const result = insertExpense.run({
    amount,
    category,
    description,
    merchant: merchant || null,
    is_income: isIncome ? 1 : 0,
    source,
    note: note || null
  });

  const cat = CATEGORIES[category];
  res.json({
    success: true,
    message: `${cat.emoji} 已记录：${description} ¥${amount.toFixed(2)} [${cat.name}]`,
    id: result.lastInsertRowid
  });
});

// --- 查询支出列表 ---
app.get('/api/expenses', authMiddleware, (req, res) => {
  const limit = Math.min(parseInt(req.query.limit) || 50, 500);
  const offset = parseInt(req.query.offset) || 0;
  const { start, end } = req.query;

  let rows;
  if (start && end) {
    rows = getExpensesByDateRange.all({ start, end });
  } else {
    rows = getExpenses.all({ limit, offset });
  }

  res.json({
    expenses: rows.map(r => ({
      ...r,
      isIncome: !!r.is_income,
      categoryName: CATEGORIES[r.category]?.name || '其他',
      categoryEmoji: CATEGORIES[r.category]?.emoji || '📦'
    })),
    total: rows.length
  });
});

// --- 删除支出 ---
app.delete('/api/expense/:id', authMiddleware, (req, res) => {
  const result = deleteExpense.run({ id: parseInt(req.params.id) });
  if (result.changes === 0) {
    return res.status(404).json({ error: '未找到该记录', code: 'NOT_FOUND' });
  }
  res.json({ success: true, message: '已删除' });
});

// --- 统计 ---
app.get('/api/stats', authMiddleware, (req, res) => {
  const period = req.query.period || 'month'; // week / month / year
  // Use SQLite's localtime for date calculations to avoid timezone issues
  const pad = n => String(n).padStart(2, '0');

  // Get current local date from SQLite to match stored dates
  const nowRow = db.prepare("SELECT date('now','localtime') as today").get();
  const today = nowRow.today; // e.g. "2026-03-29"
  const [yr, mo, dy] = today.split('-').map(Number);

  let startStr;
  if (period === 'week') {
    // Get Monday of current week
    const weekStart = db.prepare("SELECT date('now','localtime','weekday 0','-6 days') as d").get();
    startStr = weekStart.d;
  } else if (period === 'month') {
    startStr = `${yr}-${pad(mo)}-01`;
  } else if (period === 'year') {
    startStr = `${yr}-01-01`;
  } else {
    startStr = `${yr}-${pad(mo)}-01`;
  }

  const endStr = `${today} 23:59:59`;

  const categories = getCategoryTotals.all({ start: startStr, end: endStr });
  const income = getIncomeTotals.get({ start: startStr, end: endStr });
  const daily = getDailyTotals.all({ start: startStr, end: endStr });

  const totalExpense = categories.reduce((sum, c) => sum + c.total, 0);

  res.json({
    period,
    startDate: startStr,
    endDate: endStr.split(' ')[0],
    totalExpense: Math.round(totalExpense * 100) / 100,
    totalIncome: Math.round((income?.total || 0) * 100) / 100,
    expenseCount: categories.reduce((sum, c) => sum + c.count, 0),
    incomeCount: income?.count || 0,
    categories: categories.map(c => ({
      category: c.category,
      name: CATEGORIES[c.category]?.name || '其他',
      emoji: CATEGORIES[c.category]?.emoji || '📦',
      total: Math.round(c.total * 100) / 100,
      count: c.count,
      percentage: totalExpense > 0 ? Math.round(c.total / totalExpense * 10000) / 100 : 0
    })),
    dailyTrend: daily.map(d => ({
      date: d.day,
      total: Math.round(d.total * 100) / 100,
      count: d.count
    })),
    summary: buildSummaryText(categories, income, totalExpense, period)
  });
});

function buildSummaryText(categories, income, totalExpense, period) {
  const periodName = { week: '本周', month: '本月', year: '今年' }[period] || '本月';
  let text = `📊 ${periodName}支出统计\n\n`;
  text += `💸 总支出: ¥${totalExpense.toFixed(2)}\n`;
  if (income?.total) text += `💰 总收入: ¥${income.total.toFixed(2)}\n`;
  text += '\n📋 分类明细:\n';
  categories.forEach(c => {
    const cat = CATEGORIES[c.category] || CATEGORIES.OTHER;
    text += `  ${cat.emoji} ${cat.name}: ¥${c.total.toFixed(2)} (${c.count}笔)\n`;
  });
  if (categories.length === 0) text += '  暂无记录\n';
  return text;
}

// --- 月度账单（供前端账单页或 OpenClaw 查询） ---
app.get('/api/bills', authMiddleware, (req, res) => {
  const year = parseInt(req.query.year) || new Date().getFullYear();
  const month = parseInt(req.query.month); // 1-based

  if (month >= 1 && month <= 12) {
    // 具体月份
    const pad = n => String(n).padStart(2, '0');
    const startStr = `${year}-${pad(month)}-01`;
    const endStr = month === 12 ? `${year + 1}-01-01` : `${year}-${pad(month + 1)}-01`;

    const rows = db.prepare(`
      SELECT * FROM expenses WHERE created_at >= @start AND created_at < @end ORDER BY created_at DESC
    `).all({ start: startStr, end: endStr });

    const totalExpense = rows.filter(r => !r.is_income).reduce((s, r) => s + r.amount, 0);
    const totalIncome = rows.filter(r => r.is_income).reduce((s, r) => s + r.amount, 0);

    res.json({
      year, month,
      totalExpense: Math.round(totalExpense * 100) / 100,
      totalIncome: Math.round(totalIncome * 100) / 100,
      count: rows.length,
      balance: Math.round((totalIncome - totalExpense) * 100) / 100,
      expenses: rows.map(r => ({
        ...r, isIncome: !!r.is_income,
        categoryName: CATEGORIES[r.category]?.name || '其他',
        categoryEmoji: CATEGORIES[r.category]?.emoji || '📦'
      }))
    });
  } else {
    // 年度概览：返回每月汇总
    const months = [];
    for (let m = 1; m <= 12; m++) {
      const pad = n => String(n).padStart(2, '0');
      const startStr = `${year}-${pad(m)}-01`;
      const endStr = m === 12 ? `${year + 1}-01-01` : `${year}-${pad(m + 1)}-01`;

      const row = db.prepare(`
        SELECT
          COALESCE(SUM(CASE WHEN is_income = 0 THEN amount ELSE 0 END), 0) as expense,
          COALESCE(SUM(CASE WHEN is_income = 1 THEN amount ELSE 0 END), 0) as income,
          COUNT(*) as count
        FROM expenses WHERE created_at >= @start AND created_at < @end
      `).get({ start: startStr, end: endStr });

      months.push({
        month: m,
        expense: Math.round(row.expense * 100) / 100,
        income: Math.round(row.income * 100) / 100,
        count: row.count
      });
    }

    const yearExpense = months.reduce((s, m) => s + m.expense, 0);
    const yearIncome = months.reduce((s, m) => s + m.income, 0);

    res.json({
      year,
      totalExpense: Math.round(yearExpense * 100) / 100,
      totalIncome: Math.round(yearIncome * 100) / 100,
      months
    });
  }
});

// --- 类别列表 ---
app.get('/api/categories', (req, res) => {
  res.json({
    categories: Object.entries(CATEGORIES).map(([key, val]) => ({
      key,
      name: val.name,
      emoji: val.emoji,
      color: val.color
    }))
  });
});

// --- Web 前端入口 ---
app.get('/', (req, res) => {
  res.sendFile(path.join(__dirname, '..', 'chatledger-web.html'));
});

// ============= 启动 =============
app.listen(PORT, () => {
  console.log(`
╔══════════════════════════════════════════════╗
║         💰 ChatLedger Server v1.0.0         ║
╠══════════════════════════════════════════════╣
║                                              ║
║  🌐 Web UI:  http://localhost:${PORT}           ║
║  📡 API:     http://localhost:${PORT}/api       ║
║  💾 数据库:  ${DB_PATH.length > 30 ? '...' + DB_PATH.slice(-27) : DB_PATH.padEnd(30)}║
║  🔑 鉴权:    ${API_TOKEN ? '已启用' : '未启用（可设 CHATLEDGER_TOKEN）'}${' '.repeat(API_TOKEN ? 13 : 0)}║
║                                              ║
║  📖 API 文档:                                 ║
║  POST /api/record   自然语言记账（OpenClaw） ║
║  POST /api/expense  结构化记账               ║
║  GET  /api/expenses 查询支出                 ║
║  GET  /api/stats    统计数据                 ║
║  GET  /api/health   健康检查                 ║
║                                              ║
╚══════════════════════════════════════════════╝
  `);
});

// 优雅关闭
process.on('SIGINT', () => {
  console.log('\n正在关闭 ChatLedger Server...');
  db.close();
  process.exit(0);
});
process.on('SIGTERM', () => {
  db.close();
  process.exit(0);
});
