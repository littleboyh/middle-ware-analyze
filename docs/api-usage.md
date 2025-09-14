# API使用说明文档

## 访问API文档

项目启动后，可以通过以下地址访问API文档：

- **Swagger UI界面**: http://localhost:8080/middleware-analyze/swagger-ui.html
- **OpenAPI 3 JSON**: http://localhost:8080/middleware-analyze/api-docs

## 完整的业务流程示例

### 1. 提交任务

```bash
curl -X POST "http://localhost:8080/middleware-analyze/api/tasks" \
  -H "Content-Type: application/json" \
  -d '{
    "submitter": "张三",
    "description": "MySQL集群访问分析",
    "serverIps": ["10.106.60.172", "10.106.60.173"],
    "port": 3306,
    "startDate": "2025-08-01",
    "endDate": "2025-08-07"
  }'
```

**响应示例:**
```json
{
    "code": 200,
    "message": "任务提交成功",
    "data": {
        "taskId": "task_1700123456789_abc12345",
        "status": "SUBMITTED",
        "estimatedTime": "预计10-15分钟完成"
    }
}
```

### 2. 查询任务状态

```bash
curl "http://localhost:8080/middleware-analyze/api/tasks/status?taskId=task_1700123456789_abc12345"
```

**任务状态说明:**
- `SUBMITTED`: 已提交，等待处理
- `ES_QUERYING`: 正在从Elasticsearch查询数据
- `ES_COMPLETED`: ES查询完成
- `API_CALLING`: 正在调用外部API获取详细信息
- `COMPLETED`: 任务处理完成
- `FAILED`: 任务处理失败

### 3. 查询客户端IP结果

```bash
curl "http://localhost:8080/middleware-analyze/api/tasks/client-ips?taskId=task_1700123456789_abc12345"
```

### 4. 查询完整主机信息

```bash
curl "http://localhost:8080/middleware-analyze/api/tasks/host-info?taskId=task_1700123456789_abc12345"
```

### 5. 任务列表查询

```bash
# 基本查询
curl "http://localhost:8080/middleware-analyze/api/tasks?page=0&size=10"

# 带筛选条件的查询
curl "http://localhost:8080/middleware-analyze/api/tasks?page=0&size=10&submitter=张三&description=MySQL"
```

### 6. 删除任务

```bash
curl -X DELETE "http://localhost:8080/middleware-analyze/api/tasks?taskId=task_1700123456789_abc12345"
```

## API接口详情

### 任务管理接口

| 接口 | 方法 | 路径 | 描述 |
|-----|------|------|------|
| 提交任务 | POST | `/api/tasks` | 提交新的分析任务 |
| 任务列表 | GET | `/api/tasks` | 分页查询任务列表，支持筛选 |
| 全量任务 | GET | `/api/tasks/all` | 查询所有任务，不筛选 |
| 任务状态 | GET | `/api/tasks/status` | 查询任务执行状态和进度 |
| 客户端IP | GET | `/api/tasks/client-ips` | 查询ES查询结果 |
| 主机信息 | GET | `/api/tasks/host-info` | 查询完整的主机和应用信息 |
| 删除任务 | DELETE | `/api/tasks` | 删除指定任务 |

### 参数说明

#### 任务提交参数
- **submitter**: 提交人，必填，最大100字符
- **description**: 任务描述，可选，最大500字符
- **serverIps**: 服务端IP列表，必填，1-50个IP
- **port**: 服务端端口，必填，1-65535范围
- **startDate**: 开始日期，必填，格式：yyyy-MM-dd
- **endDate**: 结束日期，必填，格式：yyyy-MM-dd，最大时间跨度30天

#### 查询参数
- **page**: 页码，从0开始
- **size**: 每页数量，最大100
- **sort**: 排序字段，可选值：createTime、updateTime
- **submitter**: 提交人筛选，模糊匹配
- **description**: 任务描述筛选，模糊匹配

### 统一响应格式

所有接口都使用统一的响应格式：

```json
{
    "code": 200,           // 响应码：200成功，400客户端错误，404不存在，500服务器错误
    "message": "成功",      // 响应消息
    "data": {}             // 响应数据，失败时为null
}
```

### 错误处理

#### 常见错误码
- **400**: 参数校验失败、业务规则违反
- **404**: 资源不存在（任务不存在）
- **500**: 系统内部错误

#### 错误响应示例
```json
{
    "code": 400,
    "message": "查询时间范围不能超过30天",
    "data": null
}
```

## 数据模型说明

### ES数据结构
系统查询的Elasticsearch索引数据格式：

```json
{
    "srcip": "10.100.6.218",      // 客户端IP
    "dstip": "10.106.60.172",     // 服务端IP
    "dport": 8080,                // 服务端端口
    "sport": 50843,               // 客户端端口
    "bytes": 1522,                // 传输字节数
    "count": 1,                   // 记录数量
    "@timestamp": "2025-08-16T18:27:10.031Z"
}
```

### 查询逻辑
1. **ES查询条件**: `dstip IN serverIPs AND dport = port`
2. **聚合维度**: 按`srcip`（客户端IP）聚合
3. **时间范围**: 按天拆分查询，避免大数据量超时
4. **结果处理**: 分别处理每个服务端IP的访问情况

### 外部API集成
系统会调用外部API获取额外信息：

1. **机器列表API**: 根据IP获取主机名
2. **主机详细信息API**: 获取Linux路径和应用列表
3. **服务信息API**: 获取应用负责人和部门信息

## 最佳实践

### 1. 任务提交最佳实践
- 合理设置时间范围，避免查询过大数据量
- 使用描述性的任务描述，便于后续管理
- 服务端IP列表不要过多，建议每次不超过10个

### 2. 状态监控建议
- 对于长时间运行的任务，建议定期轮询状态接口
- 推荐轮询间隔：3-5秒
- 任务完成后及时获取结果，避免堆积

### 3. 错误处理建议
- 实现重试机制处理临时性错误
- 记录详细的错误日志用于排查
- 对于业务错误，向用户展示明确的错误信息

### 4. 性能优化建议
- 大批量查询时，考虑分批提交任务
- 避免频繁查询大量历史数据
- 及时清理不需要的历史任务

## 故障排查

### 常见问题及解决方案

1. **任务长时间停留在SUBMITTED状态**
   - 检查任务队列是否正常运行
   - 查看应用日志中的错误信息

2. **ES查询失败**
   - 确认Elasticsearch服务是否正常
   - 检查索引是否存在
   - 验证时间范围是否合理

3. **外部API调用失败**
   - 检查网络连接
   - 验证API地址配置
   - 查看API服务状态

4. **任务结果为空**
   - 确认查询条件是否正确
   - 检查时间范围内是否有数据
   - 验证服务端IP和端口是否准确

### 日志查看
应用日志位于：`logs/middleware-analyze.log`

关键日志关键词：
- `任务处理`: 任务处理相关日志
- `ES查询`: Elasticsearch查询日志
- `API调用`: 外部API调用日志
- `ERROR`: 错误日志