package com.wind.middleware.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wind.middleware.dto.external.HostDetailInfo;
import com.wind.middleware.dto.external.MachineInfo;
import com.wind.middleware.dto.external.ServiceInfo;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 外部API调用服务
 */
@Slf4j
@Service
public class ExternalAPIService {

    @Autowired
    private CloseableHttpClient httpClient;

    @Value("${middleware.external-api.machine-list-url}")
    private String machineListUrl;

    @Value("${middleware.external-api.service-info-url}")
    private String serviceInfoUrl;

    @Value("${middleware.external-api.retry-count}")
    private int retryCount;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // 缓存机器信息，避免重复调用
    private volatile Map<String, MachineInfo> machineInfoCache = new HashMap<>();
    private volatile long cacheUpdateTime = 0;
    private static final long CACHE_EXPIRE_TIME = 5 * 60 * 1000; // 5分钟缓存

    /**
     * 获取机器列表信息
     */
    public List<MachineInfo> getMachineList() {
        // 检查缓存是否过期
        if (System.currentTimeMillis() - cacheUpdateTime > CACHE_EXPIRE_TIME) {
            refreshMachineCache();
        }

        return machineInfoCache.values().stream().toList();
    }

    /**
     * 通过IP获取机器信息
     */
    public Optional<MachineInfo> getMachineByIp(String ip) {
        // 检查缓存是否过期
        if (System.currentTimeMillis() - cacheUpdateTime > CACHE_EXPIRE_TIME) {
            refreshMachineCache();
        }

        return Optional.ofNullable(machineInfoCache.get(ip));
    }

    /**
     * 刷新机器信息缓存
     */
    private synchronized void refreshMachineCache() {
        // 双重检查锁定
        if (System.currentTimeMillis() - cacheUpdateTime <= CACHE_EXPIRE_TIME) {
            return;
        }

        for (int attempt = 1; attempt <= retryCount; attempt++) {
            try {
                log.info("开始刷新机器信息缓存，第 {} 次尝试", attempt);
                HttpGet request = new HttpGet(machineListUrl);
                String response = httpClient.execute(request, this::handleResponse);

                List<MachineInfo> machines = objectMapper.readValue(response,
                    new TypeReference<List<MachineInfo>>() {});

                // 构建IP到机器信息的映射
                Map<String, MachineInfo> newCache = new HashMap<>();
                for (MachineInfo machine : machines) {
                    if (machine.getInternalIp() != null) {
                        newCache.put(machine.getInternalIp(), machine);
                    }
                    if (machine.getExternalIp() != null) {
                        newCache.put(machine.getExternalIp(), machine);
                    }
                }

                machineInfoCache = newCache;
                cacheUpdateTime = System.currentTimeMillis();
                log.info("机器信息缓存刷新成功，共 {} 条记录", newCache.size());
                return;
            } catch (Exception e) {
                log.warn("刷新机器信息缓存第 {} 次尝试失败: {}", attempt, e.getMessage());
                if (attempt == retryCount) {
                    log.error("刷新机器信息缓存失败，已重试 {} 次", retryCount, e);
                }
                // 简单延迟重试
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    /**
     * 获取主机详细信息
     */
    public Optional<HostDetailInfo> getHostDetail(String clientIp, String serverIp, Integer port) {
        for (int attempt = 1; attempt <= retryCount; attempt++) {
            try {
                String url = String.format("%s?clientIp=%s&serverIp=%s&port=%d",
                    serviceInfoUrl, clientIp, serverIp, port);

                log.debug("调用主机信息API: {}", url);
                HttpGet request = new HttpGet(url);
                String response = httpClient.execute(request, this::handleResponse);

                HostDetailInfo hostDetail = objectMapper.readValue(response, HostDetailInfo.class);
                return Optional.of(hostDetail);
            } catch (Exception e) {
                log.warn("获取主机详细信息第 {} 次尝试失败: {}, 参数: clientIp={}, serverIp={}, port={}",
                    attempt, e.getMessage(), clientIp, serverIp, port);
                if (attempt == retryCount) {
                    log.error("获取主机详细信息失败，已重试 {} 次", retryCount, e);
                    return Optional.empty();
                }
                // 简单延迟重试
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        return Optional.empty();
    }

    /**
     * 获取服务信息
     */
    public Optional<ServiceInfo> getServiceInfo(String serviceName) {
        for (int attempt = 1; attempt <= retryCount; attempt++) {
            try {
                String url = String.format("%s?serviceName=%s", serviceInfoUrl, serviceName);

                log.debug("调用服务信息API: {}", url);
                HttpGet request = new HttpGet(url);
                String response = httpClient.execute(request, this::handleResponse);

                ServiceInfo serviceInfo = objectMapper.readValue(response, ServiceInfo.class);
                return Optional.of(serviceInfo);
            } catch (Exception e) {
                log.warn("获取服务信息第 {} 次尝试失败: {}, 服务名称: {}",
                    attempt, e.getMessage(), serviceName);
                if (attempt == retryCount) {
                    log.error("获取服务信息失败，已重试 {} 次", retryCount, e);
                    return Optional.empty();
                }
                // 简单延迟重试
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        return Optional.empty();
    }

    /**
     * 处理HTTP响应
     */
    private String handleResponse(ClassicHttpResponse response) {
        try {
            int statusCode = response.getCode();
            String responseBody = EntityUtils.toString(response.getEntity(), "UTF-8");

            if (statusCode >= 200 && statusCode < 300) {
                return responseBody;
            } else {
                throw new RuntimeException("HTTP请求失败，状态码: " + statusCode + ", 响应: " + responseBody);
            }
        } catch (Exception e) {
            throw new RuntimeException("处理HTTP响应失败: " + e.getMessage(), e);
        }
    }

    /**
     * 清除缓存（测试用）
     */
    public void clearCache() {
        machineInfoCache.clear();
        cacheUpdateTime = 0;
    }
}