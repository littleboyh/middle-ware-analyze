#!/bin/bash

# 中间件分析系统 - 环境重启脚本

echo "🔄 重启中间件分析系统基础环境..."

# 进入项目根目录
cd "$(dirname "$0")/.." || exit 1

# 停止环境
echo "🛑 停止现有环境..."
./scripts/stop-env.sh

# 等待几秒
sleep 3

# 启动环境
echo "🚀 启动环境..."
./scripts/start-env.sh