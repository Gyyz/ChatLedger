#!/bin/bash
# ChatLedger Server 启动脚本
# 使用方法: chmod +x start.sh && ./start.sh

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
SERVER_DIR="$SCRIPT_DIR/server"

echo "💰 正在启动 ChatLedger..."

# 检查 Node.js
if ! command -v node &>/dev/null; then
  echo "❌ 需要安装 Node.js (v18+)"
  echo "   下载: https://nodejs.org/"
  exit 1
fi

# 安装依赖（首次运行）
if [ ! -d "$SERVER_DIR/node_modules" ]; then
  echo "📦 首次运行，安装依赖..."
  cd "$SERVER_DIR" && npm install
fi

# 启动服务
cd "$SERVER_DIR"
echo ""
node server.js
