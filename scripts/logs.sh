#!/bin/bash

# 中间件分析系统 - 日志查看脚本

# 进入项目根目录
cd "$(dirname "$0")/.." || exit 1

if [ $# -eq 0 ]; then
    echo "📋 查看所有服务日志..."
    docker-compose logs -f
elif [ "$1" = "mysql" ]; then
    echo "📊 查看MySQL日志..."
    docker-compose logs -f mysql
elif [ "$1" = "es" ] || [ "$1" = "elasticsearch" ]; then
    echo "🔍 查看Elasticsearch日志..."
    docker-compose logs -f elasticsearch
else
    echo "使用方法:"
    echo "  ./scripts/logs.sh          # 查看所有服务日志"
    echo "  ./scripts/logs.sh mysql    # 查看MySQL日志"
    echo "  ./scripts/logs.sh es       # 查看Elasticsearch日志"
fi