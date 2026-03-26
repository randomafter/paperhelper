package com.history.creation.controller;

import com.history.creation.common.Result;
import com.history.creation.dto.MaterialDTO;
import com.history.creation.service.MaterialService;
import com.history.creation.service.SparkService;
import com.history.creation.util.SparkApiUtil;
import com.history.creation.util.SparkApiUtil.SparkApiException;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RestController
@RequestMapping("/spark")
@Validated
public class SparkController {

    private final SparkService sparkService;
    private final MaterialService materialService;
    private final SparkApiUtil sparkApiUtil;
    private final ExecutorService sseExecutor = Executors.newCachedThreadPool();

    public SparkController(SparkService sparkService, MaterialService materialService, SparkApiUtil sparkApiUtil) {
        this.sparkService = sparkService;
        this.materialService = materialService;
        this.sparkApiUtil = sparkApiUtil;
    }

    @PostMapping("/generate")
    public Result<Map<String, String>> generate(@RequestBody GenerateRequest req) {
        try {
            String finalPrompt = buildPromptWithMaterials(req.getPrompt(), req.getMaterialId(), req.getMaterialIds());
            String result = req.getSystemPrompt() != null && !req.getSystemPrompt().isBlank()
                    ? sparkService.generateWithSystem(req.getSystemPrompt(), finalPrompt)
                    : sparkService.generate(finalPrompt);
            return Result.ok(Map.of("result", result));
        } catch (SparkApiException e) {
            return Result.fail(e.getCode(), e.getMessage());
        } catch (Exception e) {
            return Result.fail(500, "服务器内部错误: " + e.getMessage());
        }
    }

    @PostMapping(value = "/stream", produces = "text/event-stream")
    public SseEmitter stream(@RequestBody GenerateRequest req) {
        SseEmitter emitter = new SseEmitter(180_000L);
        sseExecutor.execute(() -> {
            try {
                String finalPrompt = buildPromptWithMaterials(req.getPrompt(), req.getMaterialId(), req.getMaterialIds());
                sparkService.generateStream(
                    req.getSystemPrompt(), finalPrompt,
                    chunk -> sendChunk(emitter, chunk),
                    () -> sendDone(emitter),
                    err -> sendError(emitter, err)
                );
            } catch (Exception e) {
                sendError(emitter, e.getMessage());
            }
        });
        return emitter;
    }

    /**
     * 多轮对话流式接口（对话面板专用）
     * POST /api/spark/stream/chat
     */
    @PostMapping(value = "/stream/chat", produces = "text/event-stream")
    public SseEmitter streamChat(@RequestBody ChatRequest req) {
        SseEmitter emitter = new SseEmitter(180_000L);
        sseExecutor.execute(() -> {
            try {
                String systemPrompt = buildSystemPromptForChat(
                        req.getSystemPrompt(), req.getMaterialIds());
                List<Map<String, String>> history = req.getMessages() != null
                        ? req.getMessages() : new ArrayList<>();
                sparkApiUtil.chatStreamWithHistory(
                    systemPrompt, history,
                    chunk -> sendChunk(emitter, chunk),
                    () -> sendDone(emitter),
                    err -> sendError(emitter, err)
                );
            } catch (Exception e) {
                sendError(emitter, e.getMessage());
            }
        });
        return emitter;
    }

    @PostMapping("/suggest-material")
    public Result<Map<String, String>> suggestMaterial(@RequestBody SuggestRequest req) {
        try {
            String result = sparkService.suggestMaterial(req.getTitle(), req.getCategory());
            return Result.ok(Map.of("content", result));
        } catch (SparkApiException e) {
            return Result.fail(e.getCode(), e.getMessage());
        } catch (Exception e) {
            return Result.fail(500, "服务器内部错误: " + e.getMessage());
        }
    }

    @PostMapping("/polish")
    public Result<Map<String, String>> polish(@RequestBody PolishRequest req) {
        try {
            String result = sparkService.polishText(req.getText());
            return Result.ok(Map.of("result", result));
        } catch (SparkApiException e) {
            return Result.fail(e.getCode(), e.getMessage());
        } catch (Exception e) {
            return Result.fail(500, "服务器内部错误: " + e.getMessage());
        }
    }

    // ── 私有辅助 ──────────────────────────────────────────────────

    private String buildPromptWithMaterials(String prompt, Long materialId, List<Long> materialIds) {
        StringBuilder sb = new StringBuilder();
        if (materialId != null) {
            try {
                MaterialDTO mat = materialService.getMaterialById(materialId, null);
                if (mat != null) sb.append("【参考素材】\n标题：").append(mat.getTitle())
                        .append("\n内容：").append(mat.getContent()).append("\n\n");
            } catch (Exception ignored) {}
        }
        if (materialIds != null && !materialIds.isEmpty()) {
            int idx = 1;
            for (Long id : materialIds) {
                try {
                    MaterialDTO mat = materialService.getMaterialById(id, null);
                    if (mat != null) sb.append("【参考素材").append(idx++).append("】\n标题：")
                            .append(mat.getTitle()).append("\n内容：").append(mat.getContent()).append("\n\n");
                } catch (Exception ignored) {}
            }
        }
        sb.append(prompt);
        return sb.toString();
    }

    private String buildSystemPromptForChat(String userContext, List<Long> materialIds) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是一位历史题材创作助手。回答时必须优先使用用户绑定上下文：素材、大纲、人物设定、编辑区内容。\n");
        sb.append("若绑定上下文与常识冲突，以绑定上下文为准；仅在信息不足时做最小必要补全。\n");
        sb.append("最终回复只输出可直接使用的正文内容，不要解释、不要前言、不要题外话。\n\n");
        if (materialIds != null && !materialIds.isEmpty()) {
            int idx = 1;
            for (Long id : materialIds) {
                try {
                    MaterialDTO mat = materialService.getMaterialById(id, null);
                    if (mat != null) sb.append("【参考素材").append(idx++).append("（最高优先级）】\n")
                            .append("标题：").append(mat.getTitle()).append("\n")
                            .append("内容：").append(mat.getContent()).append("\n\n");
                } catch (Exception ignored) {}
            }
        }
        if (userContext != null && !userContext.isBlank()) {
            sb.append("【用户绑定上下文（最高优先级）】\n").append(userContext).append("\n");
        }
        return sb.toString();
    }

    private void sendChunk(SseEmitter emitter, String chunk) {
        if (chunk == null || chunk.isEmpty()) return;
        try {
            // 为了获得稳定的“打字机”效果：即使上游一次返回大段，也按字符拆分推送
            for (int i = 0; i < chunk.length(); i++) {
                String ch = String.valueOf(chunk.charAt(i));
                emitter.send(SseEmitter.event().name("chunk").data(ch));
            }
        } catch (IOException e) {
            emitter.completeWithError(e);
        }
    }

    private void sendDone(SseEmitter emitter) {
        try { emitter.send(SseEmitter.event().name("done").data("[DONE]")); emitter.complete(); }
        catch (IOException e) { emitter.completeWithError(e); }
    }

    private void sendError(SseEmitter emitter, String msg) {
        try { emitter.send(SseEmitter.event().name("error").data(msg != null ? msg : "未知错误")); emitter.complete(); }
        catch (IOException ex) { emitter.completeWithError(ex); }
    }

    // ── DTO ───────────────────────────────────────────────────────

    public static class GenerateRequest {
        @NotBlank(message = "prompt不能为空")
        @Size(max = 8000, message = "prompt不能超过8000字")
        private String prompt;
        private String systemPrompt;
        private Long materialId;
        private List<Long> materialIds;
        public String getPrompt() { return prompt; }
        public void setPrompt(String p) { this.prompt = p; }
        public String getSystemPrompt() { return systemPrompt; }
        public void setSystemPrompt(String s) { this.systemPrompt = s; }
        public Long getMaterialId() { return materialId; }
        public void setMaterialId(Long id) { this.materialId = id; }
        public List<Long> getMaterialIds() { return materialIds; }
        public void setMaterialIds(List<Long> ids) { this.materialIds = ids; }
    }

    public static class ChatRequest {
        private String systemPrompt;
        private List<Map<String, String>> messages;
        private List<Long> materialIds;
        public String getSystemPrompt() { return systemPrompt; }
        public void setSystemPrompt(String s) { this.systemPrompt = s; }
        public List<Map<String, String>> getMessages() { return messages; }
        public void setMessages(List<Map<String, String>> m) { this.messages = m; }
        public List<Long> getMaterialIds() { return materialIds; }
        public void setMaterialIds(List<Long> ids) { this.materialIds = ids; }
    }

    public static class SuggestRequest {
        @NotBlank(message = "标题不能为空")
        private String title;
        @NotBlank(message = "分类不能为空")
        private String category;
        public String getTitle() { return title; }
        public void setTitle(String t) { this.title = t; }
        public String getCategory() { return category; }
        public void setCategory(String c) { this.category = c; }
    }

    public static class PolishRequest {
        @NotBlank(message = "文本不能为空")
        @Size(max = 5000, message = "文本不能超过5000字")
        private String text;
        public String getText() { return text; }
        public void setText(String t) { this.text = t; }
    }
}



