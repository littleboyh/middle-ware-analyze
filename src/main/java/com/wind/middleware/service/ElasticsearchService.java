package com.wind.middleware.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Elasticsearch查询服务
 */
@Slf4j
@Service
public class ElasticsearchService {

    @Autowired
    private RestHighLevelClient esClient;

    @Value("${middleware.elasticsearch.index-prefix}")
    private String indexPrefix;

    @Value("${middleware.elasticsearch.retry-count}")
    private int retryCount;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 查询访问指定服务的客户端IP
     *
     * @param serverIps 服务端IP列表
     * @param port      服务端端口
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return 客户端IP访问统计Map，key为"clientIp:serverIp"，value为访问次数
     */
    public Map<String, Long> queryClientIpAccess(List<String> serverIps, Integer port,
                                                LocalDate startDate, LocalDate endDate) {
        Map<String, Long> result = new HashMap<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd");

        // 分天查询策略
        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            String indexName = indexPrefix + currentDate.format(formatter);
            log.info("开始查询索引: {}, 服务端IP: {}, 端口: {}", indexName, serverIps, port);

            try {
                // 为每个服务端IP分别查询
                for (String serverIp : serverIps) {
                    Map<String, Long> dayResult = queryClientIpForSingleServer(indexName, serverIp, port);
                    // 合并结果，key格式为"clientIp:serverIp"
                    for (Map.Entry<String, Long> entry : dayResult.entrySet()) {
                        String key = entry.getKey() + ":" + serverIp;
                        result.merge(key, entry.getValue(), Long::sum);
                    }
                }
                log.info("索引 {} 查询完成", indexName);
            } catch (Exception e) {
                log.error("查询索引 {} 失败: {}", indexName, e.getMessage());
                // 继续处理其他天的数据，不中断整个流程
            }

            currentDate = currentDate.plusDays(1);
        }

        log.info("ES查询完成，总共获得 {} 个客户端IP-服务端IP对", result.size());
        return result;
    }

    /**
     * 查询单个服务器的客户端IP访问统计
     *
     * @param indexName 索引名称
     * @param serverIp  服务端IP
     * @param port      端口
     * @return 客户端IP访问统计
     */
    private Map<String, Long> queryClientIpForSingleServer(String indexName, String serverIp, Integer port) {
        Map<String, Long> result = new HashMap<>();

        for (int attempt = 1; attempt <= retryCount; attempt++) {
            try {
                SearchRequest searchRequest = new SearchRequest(indexName);
                SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();

                // 构建查询条件 - 查询单个服务端IP
                BoolQueryBuilder boolQuery = QueryBuilders.boolQuery()
                        .must(QueryBuilders.termQuery("dstip", serverIp))
                        .must(QueryBuilders.termQuery("dport", port));

                sourceBuilder.query(boolQuery);

                // 添加srcip聚合
                sourceBuilder.aggregation(
                        AggregationBuilders.terms("client_ips")
                                .field("srcip")
                                .size(10000) // 设置聚合结果数量限制
                );

                // 设置不返回具体文档，只要聚合结果
                sourceBuilder.size(0);
                searchRequest.source(sourceBuilder);

                // 执行查询
                SearchResponse searchResponse = esClient.search(searchRequest, RequestOptions.DEFAULT);

                // 解析聚合结果
                Terms clientIpsAgg = searchResponse.getAggregations().get("client_ips");
                for (Terms.Bucket bucket : clientIpsAgg.getBuckets()) {
                    String clientIp = bucket.getKeyAsString();
                    long count = bucket.getDocCount();
                    result.put(clientIp, count);
                }

                return result;
            } catch (Exception e) {
                log.warn("查询索引 {} 服务端IP {} 第 {} 次尝试失败: {}", indexName, serverIp, attempt, e.getMessage());
                if (attempt == retryCount) {
                    throw new RuntimeException("查询ES失败，已重试 " + retryCount + " 次", e);
                }
                // 简单的延迟重试
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("查询中断", ie);
                }
            }
        }

        return result;
    }

    /**
     * 检查ES连接状态
     */
    public boolean checkConnection() {
        try {
            return esClient.ping(RequestOptions.DEFAULT);
        } catch (Exception e) {
            log.error("ES连接检查失败: {}", e.getMessage());
            return false;
        }
    }
}