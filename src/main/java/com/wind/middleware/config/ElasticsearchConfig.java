package com.wind.middleware.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.apache.http.client.config.RequestConfig;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Elasticsearch配置类
 */
@Slf4j
@Configuration
public class ElasticsearchConfig {

    @Value("${middleware.elasticsearch.hosts}")
    private String hosts;

    @Value("${middleware.elasticsearch.connection-timeout}")
    private int connectionTimeout;

    @Value("${middleware.elasticsearch.socket-timeout}")
    private int socketTimeout;

    /**
     * 配置RestHighLevelClient客户端
     */
    @Bean
    public RestHighLevelClient restHighLevelClient() {
        // 解析hosts配置
        String[] hostArray = hosts.split(",");
        HttpHost[] httpHosts = new HttpHost[hostArray.length];

        for (int i = 0; i < hostArray.length; i++) {
            String host = hostArray[i].trim();
            String[] hostPort = host.split(":");
            String hostname = hostPort[0];
            int port = hostPort.length > 1 ? Integer.parseInt(hostPort[1]) : 9200;
            httpHosts[i] = new HttpHost(hostname, port, "http");
        }

        // 构建客户端
        RestClientBuilder builder = RestClient.builder(httpHosts);

        // 配置超时时间
        builder.setRequestConfigCallback(requestConfigBuilder ->
                requestConfigBuilder
                        .setConnectTimeout(connectionTimeout)
                        .setSocketTimeout(socketTimeout)
                        .setConnectionRequestTimeout(connectionTimeout)
        );

        // 配置HTTP客户端
        builder.setHttpClientConfigCallback(httpClientBuilder ->
                httpClientBuilder
                        .setMaxConnTotal(100)
                        .setMaxConnPerRoute(100)
        );

        RestHighLevelClient client = new RestHighLevelClient(builder);
        log.info("Elasticsearch客户端配置完成，连接地址: {}", hosts);
        return client;
    }
}