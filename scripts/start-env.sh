#!/bin/bash

# 中间件分析系统 - 环境启动脚本

echo "🚀 启动中间件分析系统基础环境..."

# 检查Docker是否运行
if ! docker info > /dev/null 2>&1; then
    echo "❌ Docker未运行，请先启动Docker"
    exit 1
fi

# 检查docker-compose是否存在
if ! command -v docker-compose &> /dev/null; then
    echo "❌ docker-compose未安装，请先安装docker-compose"
    exit 1
fi

# 进入项目根目录
cd "$(dirname "$0")/.." || exit 1

# 创建必要的目录和权限
echo "📁 创建数据目录..."
mkdir -p docker/data/mysql docker/data/elasticsearch
sudo chown -R 1000:1000 docker/data/elasticsearch || echo "⚠️  elasticsearch目录权限设置可能需要手动调整"

# 启动服务
echo "🏃 启动Docker服务..."
docker-compose up -d

# 等待服务启动
echo "⏳ 等待服务启动..."
sleep 10

# 检查服务状态
echo "🔍 检查服务状态..."

# 检查MySQL
echo "📊 检查MySQL状态..."
for i in {1..30}; do
    if docker-compose exec -T mysql mysqladmin ping -h localhost -uroot -proot123 --silent; then
        echo "✅ MySQL启动成功"
        break
    else
        echo "⏳ MySQL启动中... ($i/30)"
        sleep 5
    fi

    if [ $i -eq 30 ]; then
        echo "❌ MySQL启动超时"
        exit 1
    fi
done

# 检查Elasticsearch
echo "🔍 检查Elasticsearch状态..."
for i in {1..30}; do
    if curl -s "http://localhost:9200/_cluster/health" > /dev/null; then
        echo "✅ Elasticsearch启动成功"
        break
    else
        echo "⏳ Elasticsearch启动中... ($i/30)"
        sleep 5
    fi

    if [ $i -eq 30 ]; then
        echo "❌ Elasticsearch启动超时"
        exit 1
    fi
done

# 显示服务信息
echo ""
echo "🎉 环境启动完成！"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "📊 MySQL:        http://localhost:3306"
echo "   数据库:       middleware_analyze"
echo "   用户名:       root"
echo "   密码:         root123"
echo ""
echo "🔍 Elasticsearch: http://localhost:9200"
echo "   集群健康:     http://localhost:9200/_cluster/health"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "💡 提示:"
echo "   - 查看日志: ./scripts/logs.sh"
echo "   - 停止环境: ./scripts/stop-env.sh"
echo "   - 重启环境: ./scripts/restart-env.sh"
echo "