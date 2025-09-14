package com.wind.middleware.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.util.Timeout;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * HTTP客户端配置类
 */
@Slf4j
@Configuration
public class HttpClientConfig {

    @Value("${middleware.external-api.connection-timeout}")
    private int connectionTimeout;

    @Value("${middleware.external-api.read-timeout}")
    private int readTimeout;

    /**
     * 配置HTTP客户端
     */
    @Bean
    public CloseableHttpClient httpClient() {
        // 配置连接池
        var connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
                .setMaxConnTotal(100)
                .setMaxConnPerRoute(20)
                .build();

        // 配置请求超时
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(Timeout.ofMilliseconds(connectionTimeout))
                .setResponseTimeout(Timeout.ofMilliseconds(readTimeout))
                .build();

        CloseableHttpClient httpClient = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(requestConfig)
                .build();

        log.info("HTTP客户端配置完成，连接超时: {}ms, 读取超时: {}ms", connectionTimeout, readTimeout);
        return httpClient;
    }
}