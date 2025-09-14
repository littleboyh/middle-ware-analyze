-- 中间件分析系统数据库初始化脚本

-- 设置字符集
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 使用数据库
USE middleware_analyze;

-- 创建任务表
DROP TABLE IF EXISTS `t_task`;
CREATE TABLE `t_task` (
  `task_id` varchar(64) NOT NULL COMMENT '任务ID',
  `submitter` varchar(100) NOT NULL COMMENT '提交人',
  `description` text COMMENT '任务描述',
  `server_ips` json NOT NULL COMMENT '服务端IP列表',
  `port` int NOT NULL COMMENT '服务端端口',
  `start_date` date NOT NULL COMMENT '查询开始日期',
  `end_date` date NOT NULL COMMENT '查询结束日期',
  `status` varchar(20) NOT NULL DEFAULT 'SUBMITTED' COMMENT '任务状态',
  `error_message` text COMMENT '错误信息',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除标志',
  PRIMARY KEY (`task_id`),
  KEY `idx_submitter` (`submitter`),
  KEY `idx_status` (`status`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='任务表';

-- 创建任务结果表
DROP TABLE IF EXISTS `t_task_result`;
CREATE TABLE `t_task_result` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `task_id` varchar(64) NOT NULL COMMENT '任务ID',
  `client_ip` varchar(45) NOT NULL COMMENT '客户端IP',
  `server_ip` varchar(45) NOT NULL COMMENT '服务端IP',
  `port` int NOT NULL COMMENT '端口',
  `access_count` bigint NOT NULL DEFAULT '0' COMMENT '访问次数',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除标志',
  PRIMARY KEY (`id`),
  KEY `idx_task_id` (`task_id`),
  KEY `idx_client_ip` (`client_ip`),
  KEY `idx_server_ip` (`server_ip`),
  KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='任务结果表';

-- 创建主机信息表
DROP TABLE IF EXISTS `t_host_info`;
CREATE TABLE `t_host_info` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `task_id` varchar(64) NOT NULL COMMENT '任务ID',
  `client_ip` varchar(45) NOT NULL COMMENT '客户端IP',
  `server_ip` varchar(45) NOT NULL COMMENT '服务端IP',
  `port` int NOT NULL COMMENT '端口',
  `hostname` varchar(255) COMMENT '主机名',
  `linux_path` varchar(500) COMMENT 'Linux路径',
  `app_name` varchar(255) COMMENT '应用名称',
  `app_owner` varchar(100) COMMENT '应用负责人',
  `app_owner_account` varchar(100) COMMENT '负责人域账号',
  `department` varchar(100) COMMENT '部门',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除标志',
  PRIMARY KEY (`id`),
  KEY `idx_task_id` (`task_id`),
  KEY `idx_client_ip` (`client_ip`),
  KEY `idx_server_ip` (`server_ip`),
  KEY `idx_hostname` (`hostname`),
  KEY `idx_app_name` (`app_name`),
  KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='主机信息表';

-- 插入测试数据
INSERT INTO `t_task` (`task_id`, `submitter`, `description`, `server_ips`, `port`, `start_date`, `end_date`, `status`, `create_time`) VALUES
('task_test_001', '测试用户', '测试任务1', JSON_ARRAY('192.168.1.100', '192.168.1.101'), 8080, '2023-12-01', '2023-12-02', 'COMPLETED', NOW()),
('task_test_002', '测试用户2', '测试任务2', JSON_ARRAY('192.168.1.102'), 3306, '2023-12-02', '2023-12-03', 'SUBMITTED', NOW());

SET FOREIGN_KEY_CHECKS = 1;