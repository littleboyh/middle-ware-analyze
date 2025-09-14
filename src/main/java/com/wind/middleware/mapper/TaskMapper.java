package com.wind.middleware.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wind.middleware.entity.Task;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 任务表 Mapper 接口
 */
@Mapper
public interface TaskMapper extends BaseMapper<Task> {

    /**
     * 分页查询任务列表
     *
     * @param page       分页对象
     * @param submitter  提交人（模糊查询）
     * @param description 任务描述（模糊查询）
     * @return 分页结果
     */
    Page<Task> selectTaskPage(Page<Task> page,
                             @Param("submitter") String submitter,
                             @Param("description") String description);

    /**
     * 查询全部任务（不进行条件筛选）
     *
     * @param page 分页对象
     * @return 分页结果
     */
    Page<Task> selectAllTaskPage(Page<Task> page);
}