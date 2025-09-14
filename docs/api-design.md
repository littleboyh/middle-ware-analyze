# API接口设计文档

## 接口设计规范

### 基本规范
- **请求方式**: RESTful API设计，使用GET参数传递而非PathVariables
- **响应格式**: 统一JSON格式 `{"code": xxx, "message": "xxx", "data": object}`
- **字符编码**: UTF-8
- **时间格式**: yyyy-MM-dd HH:mm:ss
- **分页参数**: page(页码，从0开始), size(页面大小), sort(排序字段)

### 响应状态码规范
| 状态码 | 含义 | 使用场景 |
|--------|------|----------|
| 200 | 成功 | 请求处理成功 |
| 400 | 客户端错误 | 参数校验失败、业务规则违反 |
| 404 | 资源不存在 | 查询的任务不存在 |
| 500 | 服务器错误 | 系统内部异常 |

### 统一响应格式
```java
// 成功响应
{
    "code": 200,
    "message": "成功",
    "data": {实际数据}
}

// 错误响应
{
    "code": 400,
    "message": "参数校验失败",
    "data": null
}
```

## API接口列表

### 1. 提交分析任务

**接口地址**: `POST /api/tasks`

**接口描述**: 提交新的中间件分析任务

**请求参数**:
```json
{
    "submitter": "张三",                      // 提交人，必填
    "description": "MySQL集群访问分析",        // 任务描述，选填
    "serverIps": [                           // 服务端IP列表，必填
        "10.106.60.172",
        "10.106.60.173",
        "10.106.60.174"
    ],
    "port": 3306,                           // 服务端端口，必填
    "startDate": "2025-08-01",              // 查询开始日期，必填，格式：yyyy-MM-dd
    "endDate": "2025-08-07"                 // 查询结束日期，必填，格式：yyyy-MM-dd
}
```

**请求参数校验**:
- submitter: 长度1-100字符
- serverIps: 至少包含1个IP，最多50个IP
- port: 1-65535范围
- startDate/endDate: 有效日期格式，且startDate <= endDate
- 查询时间范围: 最大30天

**成功响应**:
```json
{
    "code": 200,
    "message": "任务提交成功",
    "data": {
        "taskId": "task_20250914_001",       // 任务ID
        "status": "SUBMITTED",               // 任务状态
        "estimatedTime": "预计10-30分钟完成"   // 预计处理时间
    }
}
```

**错误响应**:
```json
{
    "code": 400,
    "message": "查询时间范围不能超过30天",
    "data": null
}
```

### 2. 任务列表查询

**接口地址**: `GET /api/tasks`

**接口描述**: 查询任务列表，支持分页、排序和条件筛选

**请求参数**:
| 参数名 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|--------|------|
| page | Integer | 否 | 0 | 页码，从0开始 |
| size | Integer | 否 | 10 | 页面大小，最大100 |
| sort | String | 否 | createTime | 排序字段：createTime/updateTime |
| submitter | String | 否 | - | 提交人筛选，模糊匹配 |
| description | String | 否 | - | 任务描述筛选，模糊匹配 |

**请求示例**:
```
GET /api/tasks?page=0&size=10&sort=createTime&submitter=张三&description=MySQL
```

**成功响应**:
```json
{
    "code": 200,
    "message": "查询成功",
    "data": {
        "content": [
            {
                "taskId": "task_20250914_001",
                "submitter": "张三",
                "description": "MySQL集群访问分析",
                "serverIps": ["10.106.60.172", "10.106.60.173"],
                "port": 3306,
                "startDate": "2025-08-01",
                "endDate": "2025-08-07",
                "status": "COMPLETED",
                "createTime": "2025-09-14 10:30:00",
                "updateTime": "2025-09-14 10:45:00"
            }
        ],
        "totalElements": 25,        // 总记录数
        "totalPages": 3,           // 总页数
        "size": 10,                // 页面大小
        "number": 0,               // 当前页码
        "first": true,             // 是否首页
        "last": false              // 是否末页
    }
}
```

### 3. 全量任务查询

**接口地址**: `GET /api/tasks/all`

**接口描述**: 查询所有任务，默认按创建时间倒序排列，不进行条件筛选

**请求参数**:
| 参数名 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|--------|------|
| page | Integer | 否 | 0 | 页码，从0开始 |
| size | Integer | 否 | 20 | 页面大小，最大100 |
| sort | String | 否 | createTime | 排序字段：createTime/updateTime |

**请求示例**:
```
GET /api/tasks/all?page=0&size=20&sort=createTime
```

**成功响应**: 与任务列表查询响应格式相同

### 4. 任务状态查询

**接口地址**: `GET /api/tasks/status`

**接口描述**: 查询指定任务的执行状态和进度信息

**请求参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| taskId | String | 是 | 任务ID |

**请求示例**:
```
GET /api/tasks/status?taskId=task_20250914_001
```

**成功响应**:
```json
{
    "code": 200,
    "message": "查询成功",
    "data": {
        "taskId": "task_20250914_001",
        "status": "ES_QUERYING",                    // 任务状态
        "statusDesc": "ES查询中",                   // 状态描述
        "progress": {
            "currentStep": "正在查询第3天，共7天",     // 当前进度
            "percentage": 42.8,                     // 进度百分比
            "estimatedRemaining": "预计还需8分钟"     // 预计剩余时间
        },
        "startTime": "2025-09-14 10:30:00",        // 开始时间
        "updateTime": "2025-09-14 10:32:15",       // 最后更新时间
        "errorMessage": null                        // 错误信息（如有）
    }
}
```

**任务状态枚举**:
| 状态码 | 状态名称 | 描述 |
|--------|----------|------|
| SUBMITTED | 已提交 | 任务已提交，等待处理 |
| ES_QUERYING | ES查询中 | 正在从Elasticsearch查询数据 |
| ES_COMPLETED | ES查询完成 | ES数据查询完成 |
| API_CALLING | API调用中 | 正在调用外部API获取详细信息 |
| COMPLETED | 已完成 | 任务执行完成 |
| FAILED | 执行失败 | 任务执行过程中出现错误 |

### 5. 客户端IP访问结果查询

**接口地址**: `GET /api/tasks/client-ips`

**接口描述**: 查询任务的ES查询结果，即访问指定服务的客户端IP列表

**请求参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| taskId | String | 是 | 任务ID |

**请求示例**:
```
GET /api/tasks/client-ips?taskId=task_20250914_001
```

**成功响应**:
```json
{
    "code": 200,
    "message": "查询成功",
    "data": [
        {
            "clientIp": "10.100.6.218",
            "serverIp": "10.106.60.172",
            "port": 3306,
            "accessCount": 1245,                    // 访问次数统计
            "firstAccessTime": "2025-08-01 09:15:30", // 首次访问时间
            "lastAccessTime": "2025-08-07 18:42:15"   // 最后访问时间
        },
        {
            "clientIp": "10.100.6.219",
            "serverIp": "10.106.60.173",
            "port": 3306,
            "accessCount": 892
        }
    ]
}
```

### 6. 完整主机信息查询

**接口地址**: `GET /api/tasks/host-info`

**接口描述**: 查询任务的完整结果，包括客户端IP、主机信息、应用信息等

**请求参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| taskId | String | 是 | 任务ID |

**请求示例**:
```
GET /api/tasks/host-info?taskId=task_20250914_001
```

**成功响应**:
```json
{
    "code": 200,
    "message": "查询成功",
    "data": [
        {
            "clientIp": "10.100.6.218",
            "serverIp": "10.106.60.172",
            "port": 3306,
            "hostname": "app-server-01",            // 主机名
            "linuxPath": "/opt/applications/user-service", // Linux路径
            "applications": [                       // 访问该服务的应用列表
                {
                    "appName": "用户服务",           // 应用名称
                    "appOwner": "张三",             // 应用负责人
                    "appOwnerAccount": "zhangsan",   // 负责人域账号
                    "department": "用户中心"         // 部门
                },
                {
                    "appName": "订单服务",
                    "appOwner": "李四",
                    "appOwnerAccount": "lisi",
                    "department": "交易中心"
                }
            ]
        },
        {
            "clientIp": "10.100.6.219",
            "serverIp": "10.106.60.173",
            "port": 3306,
            "hostname": "app-server-02",
            "linuxPath": "/opt/applications/order-service",
            "applications": [
                {
                    "appName": "支付服务",
                    "appOwner": "王五",
                    "appOwnerAccount": "wangwu",
                    "department": "支付中心"
                }
            ]
        }
    ]
}
```

### 7. 任务删除

**接口地址**: `DELETE /api/tasks`

**接口描述**: 删除指定任务及其相关数据（逻辑删除）

**请求参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| taskId | String | 是 | 任务ID |

**请求示例**:
```
DELETE /api/tasks?taskId=task_20250914_001
```

**业务规则**:
- 只能删除状态为 COMPLETED 或 FAILED 的任务
- 删除任务会级联删除相关的结果数据
- 采用逻辑删除，数据不会物理删除

**成功响应**:
```json
{
    "code": 200,
    "message": "任务删除成功",
    "data": null
}
```

**错误响应**:
```json
{
    "code": 400,
    "message": "只能删除已完成或失败的任务",
    "data": null
}
```

## 错误码定义

### 业务错误码
| 错误码 | 错误信息 | 说明 |
|--------|----------|------|
| 400 | 参数校验失败 | 请求参数格式或内容不符合要求 |
| 400 | 查询时间范围不能超过30天 | 任务查询时间范围限制 |
| 400 | 服务端IP列表不能为空 | serverIps参数校验失败 |
| 400 | 只能删除已完成或失败的任务 | 任务删除业务规则限制 |
| 404 | 任务不存在 | 指定的taskId不存在或已被删除 |
| 500 | ES查询异常 | Elasticsearch查询过程中发生错误 |
| 500 | 外部API调用异常 | 调用外部接口时发生错误 |
| 500 | 系统内部错误 | 其他系统异常 |

## 接口调用示例

### 完整业务流程示例

#### 1. 提交任务
```bash
curl -X POST http://localhost:8080/middleware-analyze/api/tasks \
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

#### 2. 查询任务状态
```bash
curl "http://localhost:8080/middleware-analyze/api/tasks/status?taskId=task_20250914_001"
```

#### 3. 查询任务列表
```bash
curl "http://localhost:8080/middleware-analyze/api/tasks?page=0&size=10&submitter=张三"
```

#### 4. 查询结果
```bash
# 查询客户端IP结果
curl "http://localhost:8080/middleware-analyze/api/tasks/client-ips?taskId=task_20250914_001"

# 查询完整主机信息
curl "http://localhost:8080/middleware-analyze/api/tasks/host-info?taskId=task_20250914_001"
```

#### 5. 删除任务
```bash
curl -X DELETE "http://localhost:8080/middleware-analyze/api/tasks?taskId=task_20250914_001"
```

## 前端集成建议

### 统一响应处理
```javascript
// 前端统一响应处理函数
function handleResponse(response) {
    if (response.code === 200) {
        return response.data;
    } else {
        throw new Error(response.message);
    }
}

// 使用示例
fetch('/api/tasks', {
    method: 'GET'
})
.then(res => res.json())
.then(handleResponse)
.then(data => {
    // 处理成功数据
})
.catch(error => {
    // 处理错误信息
    console.error('API调用失败:', error.message);
});
```

### 分页组件集成
```javascript
// 分页参数构造
const pageParams = {
    page: currentPage,
    size: pageSize,
    sort: 'createTime',
    submitter: filterSubmitter,
    description: filterDescription
};

const queryString = new URLSearchParams(pageParams).toString();
const apiUrl = `/api/tasks?${queryString}`;
```

### 状态轮询示例
```javascript
// 任务状态轮询
function pollTaskStatus(taskId) {
    const poll = () => {
        fetch(`/api/tasks/status?taskId=${taskId}`)
            .then(res => res.json())
            .then(handleResponse)
            .then(data => {
                updateTaskStatus(data);

                if (data.status === 'COMPLETED' || data.status === 'FAILED') {
                    clearInterval(pollingInterval);
                }
            })
            .catch(error => {
                console.error('状态查询失败:', error);
                clearInterval(pollingInterval);
            });
    };

    const pollingInterval = setInterval(poll, 3000); // 每3秒轮询一次
    poll(); // 立即执行一次
}
```