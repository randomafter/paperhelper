package com.history.creation.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "spark")
public class SparkProperties {
    private String appId;
    private String apiKey;
    private String apiSecret;
    private String apiUrl = "https://spark-api-open.xf-yun.com/v1/chat/completions";
    private String model = "generalv3.5";
    private int maxTokens = 4096;
    private int timeoutSeconds = 60;
}
