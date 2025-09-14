package com.wind.middleware.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wind.middleware.entity.HostInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 主机信息表 Mapper 接口
 */
@Mapper
public interface HostInfoMapper extends BaseMapper<HostInfo> {

    /**
     * 批量插入主机信息
     *
     * @param hostInfoList 主机信息列表
     * @return 插入条数
     */
    int batchInsert(@Param("list") List<HostInfo> hostInfoList);

    /**
     * 根据任务ID查询主机信息
     *
     * @param taskId 任务ID
     * @return 主机信息列表
     */
    List<HostInfo> selectByTaskId(@Param("taskId") String taskId);

    /**
     * 根据任务ID删除主机信息（逻辑删除）
     *
     * @param taskId 任务ID
     * @return 删除条数
     */
    int deleteByTaskId(@Param("taskId") String taskId);

    /**
     * 根据客户端IP和服务端IP查询主机信息
     *
     * @param taskId   任务ID
     * @param clientIp 客户端IP
     * @param serverIp 服务端IP
     * @param port     端口
     * @return 主机信息
     */
    HostInfo selectByIpAndPort(@Param("taskId") String taskId,
                              @Param("clientIp") String clientIp,
                              @Param("serverIp") String serverIp,
                              @Param("port") Integer port);
}