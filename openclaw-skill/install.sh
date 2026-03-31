#!/bin/bash
# ChatLedger OpenClaw Skill 安装脚本
# 将技能安装到 OpenClaw 的 skills 目录

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
SKILL_DIR="$HOME/.openclaw/skills/chatledger"

echo "💰 安装 ChatLedger OpenClaw Skill..."

# 创建目录
mkdir -p "$SKILL_DIR"

# 复制 SKILL.md
cp "$SCRIPT_DIR/SKILL.md" "$SKILL_DIR/SKILL.md"

echo "✅ 安装成功！"
echo ""
echo "📝 配置步骤："
echo "1. 确保 ChatLedger Server 正在运行 (./start.sh)"
echo "2. 设置环境变量："
echo "   export CHATLEDGER_URL=http://localhost:3210"
echo "   # 可选: export CHATLEDGER_TOKEN=your-token"
echo ""
echo "3. 在 OpenClaw 中测试："
echo '   说 "午饭花了35块" 就会自动记账！'
echo ""
echo "📁 技能已安装到: $SKILL_DIR"
