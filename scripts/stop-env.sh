#!/bin/bash

# 中间件分析系统 - 环境停止脚本

echo "🛑 停止中间件分析系统基础环境..."

# 进入项目根目录
cd "$(dirname "$0")/.." || exit 1

# 停止服务
echo "🏃 停止Docker服务..."
docker-compose down

echo "🧹 清理未使用的Docker资源..."
docker system prune -f

echo "✅ 环境已停止"
echo ""
echo "💡 提示:"
echo "   - 启动环境: ./scripts/start-env.sh"
echo "   - 完全重置: docker-compose down -v (会删除所有数据)"