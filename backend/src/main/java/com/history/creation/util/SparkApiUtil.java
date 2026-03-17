package com.history.creation.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.history.creation.config.SparkProperties;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * 讯飞星火大模型 HTTP API 工具类
 * 使用 APIPassword 作为 Bearer Token
 * 接口文档: https://www.xfyun.cn/doc/spark/HTTP调用文档.html
 */
@Component
public class SparkApiUtil {

    private static final Logger log = LoggerFactory.getLogger(SparkApiUtil.class);
    private static final MediaType JSON_TYPE = MediaType.get("application/json; charset=utf-8");

    private final SparkProperties props;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    public SparkApiUtil(SparkProperties props) {
        this.props = props;
        this.objectMapper = new ObjectMapper();
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(props.getTimeoutSeconds(), TimeUnit.SECONDS)
                .readTimeout(props.getTimeoutSeconds(), TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    /**
     * 发送对话请求（非流式）
     *
     * @param systemPrompt 系统提示词（可为 null）
     * @param userMessage  用户输入
     * @return 模型回复文本
     * @throws SparkApiException API 调用异常
     */
    public String chat(String systemPrompt, String userMessage) throws SparkApiException {
        String requestBody = buildRequestBody(systemPrompt, userMessage, false);
        log.debug("[Spark] 请求URL: {}", props.getApiUrl());
        log.debug("[Spark] 请求体: {}", requestBody);

        Request request = new Request.Builder()
                .url(props.getApiUrl())
                .addHeader("Authorization", "Bearer " + props.getApiKey())
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(requestBody, JSON_TYPE))
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            String respBody = response.body() != null ? response.body().string() : "";
            if (!response.isSuccessful()) {
                log.error("[Spark] HTTP 错误 {}: {}", response.code(), respBody);
                throw new SparkApiException(response.code(), "API请求失败: " + parseErrorMessage(respBody));
            }
            log.debug("[Spark] 响应: {}", respBody);
            return parseResponse(respBody);
        } catch (IOException e) {
            log.error("[Spark] 网络异常: {}", e.getMessage());
            throw new SparkApiException(503, "网络连接超时或异常: " + e.getMessage());
        }
    }

    /**
     * 流式请求，拼接完整结果返回
     */
    public String chatStream(String systemPrompt, String userMessage) throws SparkApiException {
        String requestBody = buildRequestBody(systemPrompt, userMessage, true);

        Request request = new Request.Builder()
                .url(props.getApiUrl())
                .addHeader("Authorization", "Bearer " + props.getApiKey())
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(requestBody, JSON_TYPE))
                .build();

        StringBuilder sb = new StringBuilder();
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errBody = response.body() != null ? response.body().string() : "";
                throw new SparkApiException(response.code(), "API请求失败: " + parseErrorMessage(errBody));
            }
            if (response.body() == null) throw new SparkApiException(500, "响应体为空");
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(response.body().byteStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("data: ")) {
                        String data = line.substring(6).trim();
                        if ("[DONE]".equals(data)) break;
                        String chunk = parseStreamChunk(data);
                        if (chunk != null) sb.append(chunk);
                    }
                }
            }
        } catch (IOException e) {
            throw new SparkApiException(503, "网络连接超时或异常: " + e.getMessage());
        }
        return sb.toString();
    }

    // ── 私有方法 ──────────────────────────────────────────────────

    private String buildRequestBody(String systemPrompt, String userMessage, boolean stream) {
        try {
            ObjectNode root = objectMapper.createObjectNode();
            root.put("model", props.getModel());
            root.put("stream", stream);
            root.put("max_tokens", props.getMaxTokens());

            ArrayNode messages = objectMapper.createArrayNode();
            if (systemPrompt != null && !systemPrompt.isBlank()) {
                ObjectNode sys = objectMapper.createObjectNode();
                sys.put("role", "system");
                sys.put("content", systemPrompt);
                messages.add(sys);
            }
            ObjectNode user = objectMapper.createObjectNode();
            user.put("role", "user");
            user.put("content", userMessage);
            messages.add(user);
            root.set("messages", messages);
            return objectMapper.writeValueAsString(root);
        } catch (Exception e) {
            throw new RuntimeException("构建请求体失败", e);
        }
    }

    private String parseResponse(String json) throws SparkApiException {
        try {
            JsonNode root = objectMapper.readTree(json);
            if (root.has("code") && root.get("code").asInt() != 0) {
                throw new SparkApiException(root.get("code").asInt(),
                        root.path("message").asText("未知错误"));
            }
            JsonNode content = root.path("choices").path(0).path("message").path("content");
            if (content.isMissingNode()) {
                throw new SparkApiException(500, "响应格式异常: " + json);
            }
            return content.asText();
        } catch (SparkApiException e) {
            throw e;
        } catch (Exception e) {
            throw new SparkApiException(500, "响应解析失败: " + e.getMessage());
        }
    }

    private String parseStreamChunk(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode delta = root.path("choices").path(0).path("delta").path("content");
            return delta.isMissingNode() ? null : delta.asText();
        } catch (Exception e) {
            log.warn("[Spark] 解析流式块失败: {}", json);
            return null;
        }
    }

    private String parseErrorMessage(String body) {
        try {
            JsonNode root = objectMapper.readTree(body);
            if (root.has("message")) return root.get("message").asText();
            if (root.has("error")) return root.path("error").path("message").asText(body);
        } catch (Exception ignored) {}
        return body.length() > 200 ? body.substring(0, 200) : body;
    }

    // ── 自定义异常 ────────────────────────────────────────────────

    public static class SparkApiException extends Exception {
        private final int code;
        public SparkApiException(int code, String message) {
            super(message);
            this.code = code;
        }
        public int getCode() { return code; }
    }
}
