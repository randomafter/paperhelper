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
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

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

    public String chat(String systemPrompt, String userMessage) throws SparkApiException {
        String requestBody = buildRequestBody(systemPrompt, userMessage, false);
        Request request = new Request.Builder()
                .url(props.getApiUrl())
                .addHeader("Authorization", "Bearer " + props.getApiKey())
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(requestBody, JSON_TYPE))
                .build();
        try (Response response = httpClient.newCall(request).execute()) {
            String respBody = response.body() != null ? response.body().string() : "";
            if (!response.isSuccessful()) throw new SparkApiException(response.code(), "API请求失败: " + parseErrorMessage(respBody));
            return parseResponse(respBody);
        } catch (IOException e) {
            throw new SparkApiException(503, "网络连接超时或异常: " + e.getMessage());
        }
    }

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
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.body().byteStream(), StandardCharsets.UTF_8))) {
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

    /** 单轮流式回调（旧接口保持兼容） */
    public void chatStreamCallback(String systemPrompt, String userMessage,
                                   Consumer<String> onChunk, Runnable onDone, Consumer<String> onError) {
        String requestBody = buildRequestBody(systemPrompt, userMessage, true);
        doStreamRequest(requestBody, "[Spark Stream]", onChunk, onDone, onError);
    }

    /** 多轮对话流式回调（对话面板专用） */
    public void chatStreamWithHistory(String systemPrompt,
                                      List<Map<String, String>> history,
                                      Consumer<String> onChunk,
                                      Runnable onDone,
                                      Consumer<String> onError) {
        String requestBody = buildRequestBodyWithHistory(systemPrompt, history, true);
        doStreamRequest(requestBody, "[Spark Chat]", onChunk, onDone, onError);
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

    private String buildRequestBodyWithHistory(String systemPrompt,
                                               List<Map<String, String>> history,
                                               boolean stream) {
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
            // 最多保留最近 20 条，避免超出 token 限制
            int start = Math.max(0, history.size() - 20);
            for (int i = start; i < history.size(); i++) {
                Map<String, String> msg = history.get(i);
                ObjectNode node = objectMapper.createObjectNode();
                node.put("role", msg.getOrDefault("role", "user"));
                node.put("content", msg.getOrDefault("content", ""));
                messages.add(node);
            }
            root.set("messages", messages);
            return objectMapper.writeValueAsString(root);
        } catch (Exception e) {
            throw new RuntimeException("构建多轮请求体失败", e);
        }
    }

    private void doStreamRequest(String requestBody, String logTag,
                                 Consumer<String> onChunk, Runnable onDone, Consumer<String> onError) {
        Request request = new Request.Builder()
                .url(props.getApiUrl())
                .addHeader("Authorization", "Bearer " + props.getApiKey())
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(requestBody, JSON_TYPE))
                .build();
        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                log.error("{} 网络异常: {}", logTag, e.getMessage());
                onError.accept("网络连接异常: " + e.getMessage());
            }
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    String errBody = response.body() != null ? response.body().string() : "";
                    onError.accept("API请求失败: " + parseErrorMessage(errBody));
                    return;
                }
                if (response.body() == null) { onError.accept("响应体为空"); return; }
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(response.body().byteStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.startsWith("data: ")) {
                            String data = line.substring(6).trim();
                            if ("[DONE]".equals(data)) break;
                            String chunk = parseStreamChunk(data);
                            if (chunk != null && !chunk.isEmpty()) onChunk.accept(chunk);
                        }
                    }
                    onDone.run();
                } catch (IOException e) {
                    log.error("{} 读取流异常: {}", logTag, e.getMessage());
                    onError.accept("读取响应流异常: " + e.getMessage());
                }
            }
        });
    }

    private String parseResponse(String json) throws SparkApiException {
        try {
            JsonNode root = objectMapper.readTree(json);
            if (root.has("code") && root.get("code").asInt() != 0)
                throw new SparkApiException(root.get("code").asInt(), root.path("message").asText("未知错误"));
            JsonNode content = root.path("choices").path(0).path("message").path("content");
            if (content.isMissingNode()) throw new SparkApiException(500, "响应格式异常: " + json);
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
