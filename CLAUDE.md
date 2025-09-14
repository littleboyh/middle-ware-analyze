# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a middleware analysis system built with Spring Boot 3.3.6 that analyzes network traffic data stored in Elasticsearch and provides comprehensive host information through external API integration. The system implements asynchronous task processing with a single-threaded FIFO queue for reliable processing.

## Development Environment

### Quick Start
```bash
# Start Docker services (MySQL 8.0 + Elasticsearch 6.1.2)
./scripts/start-env.sh

# Start Spring Boot application
mvn spring-boot:run

# Stop environment
./scripts/stop-env.sh
```

### Docker Environment Management
- `./scripts/start-env.sh` - Start MySQL and Elasticsearch containers
- `./scripts/stop-env.sh` - Stop all containers
- `./scripts/restart-env.sh` - Restart containers
- `./scripts/logs.sh [mysql|es]` - View container logs

## Build and Test Commands

```bash
# Compile project
mvn clean compile

# Run tests
mvn test

# Build JAR
mvn clean package

# Run application with specific profile
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

## Core Architecture

### Event-Driven Task Processing
The system uses event-driven architecture to avoid circular dependencies:

1. **TaskService** publishes `TaskSubmittedEvent` via `ApplicationEventPublisher`
2. **TaskQueueManager** listens for events and adds tasks to blocking queue
3. **TaskProcessor** processes tasks asynchronously in single thread
4. **TaskDataService** interface decouples data operations

### Key Components

#### Task Queue System
- **TaskQueueManager**: Event-driven FIFO queue manager with single-thread processing
- **TaskProcessor**: Executes ES queries and external API calls
- **TaskSubmittedEvent**: Decouples task submission from queue management

#### Data Integration
- **ElasticsearchService**: Queries sflow network traffic data with day-by-day strategy
- **ExternalAPIService**: HTTP client with caching and retry logic for machine/service info
- **TaskService**: Core business logic implementing TaskDataService interface

#### Database Layer
- Uses MyBatis-Plus 3.5.14 with pagination and logical deletion
- Three main entities: Task, TaskResult, HostInfo
- JSON field handling for server_ips list in Task entity

## API Design Conventions

### Response Format
All API responses must follow this standardized format:
```json
{
  "code": 100-500,
  "message": "string",
  "data": "object"
}
```

### Parameter Handling
- **Never use path variables** - all parameters must be passed as query parameters
- Example: `GET /api/tasks?taskId=123` not `GET /api/tasks/{taskId}`

### OpenAPI Documentation
- Swagger UI available at: http://localhost:8080/middleware-analyze/swagger-ui.html
- API docs at: http://localhost:8080/middleware-analyze/api-docs

## Configuration Management

### Application Profiles
- **dev**: Local development with Docker containers
- **prod**: Production with external MySQL/ES hosts

### Key Configuration Properties
```yaml
middleware:
  elasticsearch:
    hosts: localhost:9200
    index-prefix: sflow-
  task:
    max-query-days: 30
    queue-capacity: 1000
  external-api:
    retry-count: 2
```

## Database Schema

### Core Tables
- **t_task**: Task metadata with JSON server_ips field
- **t_task_result**: ES query results (client_ip:server_ip mappings)
- **t_host_info**: Enriched host data from external APIs

### Elasticsearch Index Structure
- Index pattern: `sflow-YYYY.MM.DD`
- Fields: `@timestamp`, `src_ip`, `dst_ip`, `src_port`, `dst_port`, `protocol`, `bytes`, `packets`

## Development Guidelines

### Code Style Requirements
- Java 17 with Lombok for boilerplate reduction
- File size limits: Java classes max 400 lines
- Max 8 files per directory level
- Use Spring events for component decoupling

### Testing Strategy
- Integration tests should verify Docker environment connectivity
- Mock external API calls in unit tests
- Test task queue processing with various scenarios

### Logging Configuration
- Application logs: `logs/middleware-analyze.log`
- Debug level for `com.wind.middleware` package in dev profile
- Structured logging with timestamps and thread info

## Environment Dependencies

### Required Services
- **MySQL 8.0**: Database with `middleware_analyze` schema
- **Elasticsearch 6.1.2**: Network traffic data storage with sflow indices
- **External APIs**: Machine and service information endpoints (configurable)

### Local Development
The Docker Compose setup provides MySQL and Elasticsearch locally. External APIs are mocked/configurable via application properties.

## Task Processing Flow

1. User submits task via REST API with server IPs, port, and date range
2. TaskService validates parameters and persists task
3. TaskSubmittedEvent triggers TaskQueueManager to queue task
4. TaskProcessor executes ES queries day-by-day for date range
5. Results enriched with external API calls for host information
6. All data persisted to database with task status updates

## Common Issues

### Elasticsearch Compatibility
- Uses ES 6.1.2 with High Level REST Client
- ARM64 Mac may show platform warnings but functions normally
- Index-level settings must be configured via API, not elasticsearch.yml

### Task Queue Management
- Single-threaded processing prevents resource contention
- Queue capacity configurable via `middleware.task.queue-capacity`
- Failed tasks retain error messages in database