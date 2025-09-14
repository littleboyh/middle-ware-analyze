# 中间件分析系统技术架构设计

## 技术栈选择

### 核心技术栈
| 技术组件 | 版本 | 选择理由 |
|---------|------|----------|
| Java | 17 | LTS版本，性能优异，现代语言特性 |
| Spring Boot | 3.3.6 | 成熟的微服务框架，自动配置简化开发 |
| MyBatis-Plus | 3.5.x | SQL可控，性能优秀，丰富的内置功能 |
| MySQL | 8.0 | 高性能关系型数据库，事务支持完善 |
| Elasticsearch Client | 6.x | 兼容线上ES 6.1.2版本，高性能搜索 |
| HikariCP | 内置 | 高性能数据库连接池 |
| Lombok | 最新 | 简化实体类代码，提高开发效率 |
| Logback | 内置 | Spring Boot默认日志框架 |

### 开发环境
- **构建工具**: Maven 3.8+
- **容器化**: Docker + Docker Compose
- **IDE**: IntelliJ IDEA
- **版本控制**: Git

## 系统架构设计

### 整体架构图
```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   前端应用      │───→│  Spring Boot    │───→│   MySQL 8.0     │
│                 │    │   Web API       │    │   数据存储       │
└─────────────────┘    └─────────────────┘    └─────────────────┘
                              │
                              ├─────────────────→ ┌─────────────────┐
                              │                   │ Elasticsearch   │
                              │                   │   6.1.2         │
                              │                   └─────────────────┘
                              ↓
                       ┌─────────────────┐
                       │   任务队列       │
                       │  (内存队列)      │
                       └─────────────────┘
                              ↓
                       ┌─────────────────┐    ┌─────────────────┐
                       │  单线程任务      │───→│   外部API       │
                       │    处理器        │    │  服务调用       │
                       └─────────────────┘    └─────────────────┘
```

### 分层架构设计

#### 表现层 (Controller Layer)
- **职责**: 接收HTTP请求，参数校验，响应格式化
- **组件**: TaskController
- **特点**: RESTful API设计，统一响应格式

#### 业务服务层 (Service Layer)
- **职责**: 业务逻辑处理，事务管理
- **组件**: TaskService, ElasticsearchService, ExternalAPIService
- **特点**: 事务边界控制，业务规则实现

#### 数据访问层 (Repository Layer)
- **职责**: 数据持久化，数据库操作
- **组件**: TaskMapper, TaskResultMapper, HostInfoMapper
- **特点**: MyBatis-Plus条件构造器，分页查询

#### 任务处理层 (Queue Layer)
- **职责**: 异步任务处理，队列管理
- **组件**: TaskQueueManager, TaskProcessor
- **特点**: 内存队列，单线程消费

## 数据库设计

### 数据库表结构

#### 任务表 (t_task)
```sql
CREATE TABLE t_task (
    task_id VARCHAR(50) PRIMARY KEY COMMENT '任务ID',
    submitter VARCHAR(100) NOT NULL COMMENT '提交人',
    description TEXT COMMENT '任务描述',
    server_ips JSON NOT NULL COMMENT '服务端IP列表',
    port INT NOT NULL COMMENT '服务端端口',
    start_date DATE NOT NULL COMMENT '查询开始日期',
    end_date DATE NOT NULL COMMENT '查询结束日期',
    status VARCHAR(50) NOT NULL COMMENT '任务状态',
    error_message TEXT COMMENT '错误信息',
    create_time DATETIME NOT NULL COMMENT '创建时间',
    update_time DATETIME NOT NULL COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除标志',

    INDEX idx_submitter (submitter),
    INDEX idx_create_time (create_time),
    INDEX idx_status (status),
    INDEX idx_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务表';
```

#### 任务结果表 (t_task_result)
```sql
CREATE TABLE t_task_result (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id VARCHAR(50) NOT NULL COMMENT '任务ID',
    client_ip VARCHAR(15) NOT NULL COMMENT '客户端IP',
    server_ip VARCHAR(15) NOT NULL COMMENT '服务端IP',
    port INT NOT NULL COMMENT '端口',
    access_count BIGINT DEFAULT 1 COMMENT '访问次数',
    create_time DATETIME NOT NULL COMMENT '创建时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除标志',

    FOREIGN KEY (task_id) REFERENCES t_task(task_id) ON DELETE CASCADE,
    INDEX idx_task_id (task_id),
    INDEX idx_client_ip (client_ip),
    INDEX idx_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务结果表';
```

#### 主机信息表 (t_host_info)
```sql
CREATE TABLE t_host_info (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id VARCHAR(50) NOT NULL COMMENT '任务ID',
    client_ip VARCHAR(15) NOT NULL COMMENT '客户端IP',
    server_ip VARCHAR(15) NOT NULL COMMENT '服务端IP',
    port INT NOT NULL COMMENT '端口',
    hostname VARCHAR(200) COMMENT '主机名',
    linux_path VARCHAR(500) COMMENT 'Linux路径',
    app_name VARCHAR(200) COMMENT '应用名称',
    app_owner VARCHAR(100) COMMENT '应用负责人',
    app_owner_account VARCHAR(100) COMMENT '负责人域账号',
    department VARCHAR(200) COMMENT '部门',
    create_time DATETIME NOT NULL COMMENT '创建时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除标志',

    FOREIGN KEY (task_id) REFERENCES t_task(task_id) ON DELETE CASCADE,
    INDEX idx_task_id (task_id),
    INDEX idx_client_ip (client_ip),
    INDEX idx_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='主机信息表';
```

### 数据关系图
```
t_task (1) ───── (N) t_task_result
   │
   └─────────── (N) t_host_info
```

## 任务队列设计

### 队列架构
```java
// 队列管理器设计
public class TaskQueueManager {
    // 使用 LinkedBlockingQueue 保证FIFO顺序
    private final BlockingQueue<String> taskQueue = new LinkedBlockingQueue<>();

    // 单线程执行器
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    // 队列状态监控
    private volatile boolean isRunning = true;
}
```

### 任务处理流程
```
任务提交 → 入队列 → 单线程消费 → 状态更新
    ↓
ES查询 → API调用 → 结果存储 → 任务完成
```

### 异常处理策略
1. **ES查询异常**:
   - 连接超时: 重试3次
   - 索引不存在: 跳过该天，继续处理
   - 其他异常: 记录错误，标记部分失败

2. **API调用异常**:
   - 网络超时: 重试2次
   - HTTP 4xx错误: 跳过该IP
   - HTTP 5xx错误: 记录错误，继续处理

## 配置管理

### 应用配置 (application.yml)
```yaml
server:
  port: 8080
  servlet:
    context-path: /middleware-analyze

spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/middleware_analyze?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai
    username: root
    password: root123
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      idle-timeout: 300000
      max-lifetime: 900000

  jackson:
    date-format: yyyy-MM-dd HH:mm:ss
    time-zone: GMT+8

logging:
  level:
    com.wind.middleware: DEBUG
    org.springframework: INFO
  pattern:
    file: "%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"
  file:
    name: logs/middleware-analyze.log
    max-size: 100MB
    max-history: 30

# 自定义配置
middleware:
  elasticsearch:
    hosts: localhost:9200
    connection-timeout: 5000
    socket-timeout: 10000
    retry-count: 3

  external-api:
    machine-list-url: http://api.example.com/machines
    service-info-url: http://api.example.com/services
    connection-timeout: 3000
    read-timeout: 5000
    retry-count: 2

  task:
    max-query-days: 30
    queue-capacity: 1000

mybatis-plus:
  configuration:
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
  global-config:
    db-config:
      logic-delete-field: deleted
      logic-delete-value: 1
      logic-not-delete-value: 0
```

## Docker环境设计

### Docker Compose配置
```yaml
version: '3.8'
services:
  mysql:
    image: mysql:8.0
    container_name: middleware-mysql
    environment:
      MYSQL_ROOT_PASSWORD: root123
      MYSQL_DATABASE: middleware_analyze
      MYSQL_USER: middleware
      MYSQL_PASSWORD: middleware123
    ports:
      - "3306:3306"
    volumes:
      - ./docker/mysql/init.sql:/docker-entrypoint-initdb.d/init.sql
      - mysql_data:/var/lib/mysql
    restart: unless-stopped

  elasticsearch:
    image: elasticsearch:6.1.2
    container_name: middleware-es
    environment:
      - discovery.type=single-node
      - "ES_JAVA_OPTS=-Xms512m -Xmx512m"
      - xpack.security.enabled=false
    ports:
      - "9200:9200"
      - "9300:9300"
    volumes:
      - ./docker/elasticsearch/elasticsearch.yml:/usr/share/elasticsearch/config/elasticsearch.yml
      - es_data:/usr/share/elasticsearch/data
    restart: unless-stopped

volumes:
  mysql_data:
  es_data:
```

## 项目目录结构

```
middle-ware-analyze/
├── docs/                              # 项目文档
│   ├── requirements.md               # 需求文档
│   ├── architecture.md              # 架构设计文档
│   ├── api-design.md                # API接口设计
│   └── development-plan.md          # 开发计划
├── docker/                           # Docker环境配置
│   ├── docker-compose.yml           # Docker编排文件
│   ├── mysql/
│   │   └── init.sql                 # MySQL初始化脚本
│   └── elasticsearch/
│       ├── elasticsearch.yml        # ES配置文件
│       └── test-data.json           # 测试数据
├── scripts/                          # 启停脚本
│   ├── start.sh                     # 启动应用
│   ├── stop.sh                      # 停止应用
│   └── docker-env.sh                # Docker环境管理
├── logs/                            # 日志目录
├── src/main/java/com/wind/middleware/
│   ├── MiddlewareAnalyzeApplication.java  # 主启动类
│   ├── config/                      # 配置类
│   │   ├── MyBatisPlusConfig.java   # MyBatis-Plus配置
│   │   ├── ElasticsearchConfig.java # ES客户端配置
│   │   └── TaskQueueConfig.java     # 任务队列配置
│   ├── entity/                      # 实体类
│   │   ├── Task.java               # 任务实体
│   │   ├── TaskResult.java         # 任务结果实体
│   │   ├── HostInfo.java           # 主机信息实体
│   │   └── enums/
│   │       └── TaskStatus.java     # 任务状态枚举
│   ├── mapper/                      # MyBatis-Plus Mapper
│   │   ├── TaskMapper.java
│   │   ├── TaskResultMapper.java
│   │   └── HostInfoMapper.java
│   ├── service/                     # 业务服务层
│   │   ├── TaskService.java        # 任务管理服务
│   │   ├── ElasticsearchService.java # ES查询服务
│   │   ├── ExternalAPIService.java  # 外部API调用服务
│   │   └── AsyncTaskProcessor.java  # 异步任务处理器
│   ├── controller/                  # API控制器
│   │   └── TaskController.java      # 任务管理控制器
│   ├── dto/                         # 数据传输对象
│   │   ├── request/                 # 请求DTO
│   │   ├── response/                # 响应DTO
│   │   └── external/                # 外部API DTO
│   ├── queue/                       # 任务队列
│   │   ├── TaskQueueManager.java    # 队列管理器
│   │   └── TaskProcessor.java       # 任务处理器
│   ├── exception/                   # 异常处理
│   │   ├── GlobalExceptionHandler.java
│   │   └── BusinessException.java
│   └── util/                        # 工具类
│       ├── DateUtil.java
│       └── HttpUtil.java
├── src/main/resources/
│   ├── application.yml              # 应用配置
│   └── logback-spring.xml          # 日志配置
├── src/test/                        # 测试代码
├── pom.xml                          # Maven配置
└── README.md                        # 项目说明
```

## 性能优化策略

### 数据库优化
1. **索引设计**: 基于查询条件建立合适索引
2. **连接池配置**: HikariCP连接池优化
3. **分页查询**: 避免大数据量查询
4. **逻辑删除**: 保证数据安全性

### ES查询优化
1. **分天查询**: 避免跨大时间范围查询超时
2. **聚合查询**: 使用terms聚合提高性能
3. **连接管理**: 合理配置ES客户端连接参数
4. **重试机制**: 针对网络异常的重试策略

### 应用层优化
1. **单线程处理**: 避免并发竞争，保证数据一致性
2. **内存队列**: 高性能的任务队列实现
3. **异常隔离**: 异常情况下继续处理其他数据
4. **日志输出**: 结构化日志便于问题排查

## 监控和运维

### 日志管理
- **应用日志**: 输出到 `logs/middleware-analyze.log`
- **日志轮转**: 单文件最大100MB，保留30天
- **日志级别**: 开发环境DEBUG，生产环境INFO

### 健康检查
- **数据库连接**: 定期检查MySQL连接状态
- **ES连接**: 监控ES集群健康状态
- **队列状态**: 监控任务队列长度和处理情况

### 错误处理
- **全局异常处理**: 统一异常响应格式
- **业务异常**: 明确的错误码和错误信息
- **系统异常**: 详细的错误日志记录