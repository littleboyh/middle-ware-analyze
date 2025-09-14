package com.wind.middleware.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wind.middleware.entity.TaskResult;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 任务结果表 Mapper 接口
 */
@Mapper
public interface TaskResultMapper extends BaseMapper<TaskResult> {

    /**
     * 批量插入任务结果
     *
     * @param taskResults 任务结果列表
     * @return 插入条数
     */
    int batchInsert(@Param("list") List<TaskResult> taskResults);

    /**
     * 根据任务ID查询客户端IP访问结果
     *
     * @param taskId 任务ID
     * @return 客户端IP访问结果列表
     */
    List<TaskResult> selectByTaskId(@Param("taskId") String taskId);

    /**
     * 根据任务ID删除结果（逻辑删除）
     *
     * @param taskId 任务ID
     * @return 删除条数
     */
    int deleteByTaskId(@Param("taskId") String taskId);
}