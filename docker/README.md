# 中间件分析系统 - Docker环境

## 环境组件

### MySQL 8.0
- **端口**: 3306
- **数据库**: middleware_analyze
- **用户**: root/root123
- **字符集**: utf8mb4
- **时区**: Asia/Shanghai

### Elasticsearch 6.1.2
- **端口**: 9200 (HTTP), 9300 (Transport)
- **集群名**: middleware-es-cluster
- **节点名**: middleware-es-node
- **内存**: 512MB

## 快速启动

```bash
# 启动环境
./scripts/start-env.sh

# 停止环境
./scripts/stop-env.sh

# 重启环境
./scripts/restart-env.sh

# 查看日志
./scripts/logs.sh          # 所有服务
./scripts/logs.sh mysql    # MySQL日志
./scripts/logs.sh es       # Elasticsearch日志
```

## 数据库表结构

### t_task (任务表)
- task_id: 任务ID (主键)
- submitter: 提交人
- description: 任务描述
- server_ips: 服务端IP列表 (JSON)
- port: 端口
- start_date/end_date: 查询时间范围
- status: 任务状态
- create_time/update_time: 时间戳

### t_task_result (任务结果表)
- id: 主键
- task_id: 关联任务ID
- client_ip: 客户端IP
- server_ip: 服务端IP
- port: 端口
- access_count: 访问次数

### t_host_info (主机信息表)
- id: 主键
- task_id: 关联任务ID
- client_ip/server_ip: IP信息
- hostname: 主机名
- linux_path: Linux路径
- app_name: 应用名称
- app_owner: 负责人信息
- department: 部门

## Elasticsearch索引

### sflow-YYYY.MM.DD
网络流量数据索引结构:
```json
{
  "@timestamp": "2023-12-01T10:00:00",
  "src_ip": "192.168.1.10",
  "dst_ip": "192.168.1.100",
  "src_port": 55432,
  "dst_port": 8080,
  "protocol": "TCP",
  "bytes": 1024,
  "packets": 5
}
```

## 验证环境

### 验证MySQL
```bash
docker-compose exec mysql mysql -h127.0.0.1 -uroot -proot123 -e "SHOW DATABASES;"
```

### 验证Elasticsearch
```bash
curl "http://localhost:9200/_cluster/health?pretty"
curl "http://localhost:9200/_cat/indices?v"
```

## Spring Boot连接配置

应用已配置为连接到本地环境:
- MySQL: jdbc:mysql://localhost:3306/middleware_analyze
- Elasticsearch: localhost:9200

## 故障排除

### MySQL连接问题
- 等待容器完全启动(约30-60秒)
- 检查健康状态: `docker-compose ps`
- 查看日志: `./scripts/logs.sh mysql`

### Elasticsearch启动问题
- 确保没有端口冲突
- ARM架构Mac可能有兼容性警告，但不影响使用
- 检查内存设置是否合适

### 数据持久化
数据存储在 `docker/data/` 目录:
- `docker/data/mysql/`: MySQL数据文件
- `docker/data/elasticsearch/`: ES数据文件

⚠️ **注意**: 删除这些目录会导致数据丢失