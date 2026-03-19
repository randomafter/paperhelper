package com.history.creation.controller;

import com.history.creation.common.Result;
import com.history.creation.dto.MaterialDTO;
import com.history.creation.service.MaterialService;
import com.history.creation.service.SparkService;
import com.history.creation.util.SparkApiUtil.SparkApiException;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 讯飞星火大模型接口
 */
@RestController
@RequestMapping("/spark")
@Validated
public class SparkController {

    private final SparkService sparkService;
    private final MaterialService materialService;
    // 用于异步处理 SSE 流
    private final ExecutorService sseExecutor = Executors.newCachedThreadPool();

    public SparkController(SparkService sparkService, MaterialService materialService) {
        this.sparkService = sparkService;
        this.materialService = materialService;
    }

    /**
     * 通用文本生成（同步）
     * POST /api/spark/generate
     */
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

    /**
     * SSE 流式文本生成
     * POST /api/spark/stream
     * 返回 text/event-stream，每个事件携带一段文字
     */
    @PostMapping(value = "/stream", produces = "text/event-stream")
    public SseEmitter stream(@RequestBody GenerateRequest req) {
        // timeout 设置为 3 分钟（足够长的生成时间）
        SseEmitter emitter = new SseEmitter(180_000L);
        sseExecutor.execute(() -> {
            try {
                String finalPrompt = buildPromptWithMaterials(req.getPrompt(), req.getMaterialId(), req.getMaterialIds());
                String systemPrompt = req.getSystemPrompt();

                sparkService.generateStream(
                    systemPrompt,
                    finalPrompt,
                    chunk -> {
                        try {
                            // 每个 chunk 作为一个 SSE data 事件发送
                            emitter.send(SseEmitter.event()
                                .name("chunk")
                                .data(chunk));
                        } catch (IOException e) {
                            emitter.completeWithError(e);
                        }
                    },
                    () -> {
                        try {
                            // 发送结束标记
                            emitter.send(SseEmitter.event().name("done").data("[DONE]"));
                            emitter.complete();
                        } catch (IOException e) {
                            emitter.completeWithError(e);
                        }
                    },
                    errMsg -> {
                        try {
                            emitter.send(SseEmitter.event().name("error").data(errMsg));
                            emitter.complete();
                        } catch (IOException ex) {
                            emitter.completeWithError(ex);
                        }
                    }
                );
            } catch (Exception e) {
                try {
                    emitter.send(SseEmitter.event().name("error").data(e.getMessage()));
                    emitter.complete();
                } catch (IOException ex) {
                    emitter.completeWithError(ex);
                }
            }
        });
        return emitter;
    }

    /**
     * 生成历史素材内容建议
     * POST /api/spark/suggest-material
     */
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

    /**
     * 文章润色
     * POST /api/spark/polish
     */
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

    // ── 私有辅助方法 ──────────────────────────────────────────────

    private String buildPromptWithMaterials(String prompt, Long materialId, java.util.List<Long> materialIds) {
        StringBuilder sb = new StringBuilder();
        // 单素材（向后兼容）
        if (materialId != null) {
            try {
                MaterialDTO mat = materialService.getMaterialById(materialId, null);
                if (mat != null) {
                    sb.append("【参考素材】\n标题：").append(mat.getTitle())
                      .append("\n内容：").append(mat.getContent()).append("\n\n");
                }
            } catch (Exception ignored) {}
        }
        // 多素材
        if (materialIds != null && !materialIds.isEmpty()) {
            int idx = 1;
            for (Long id : materialIds) {
                try {
                    MaterialDTO mat = materialService.getMaterialById(id, null);
                    if (mat != null) {
                        sb.append("【参考素材").append(idx++).append("】\n标题：").append(mat.getTitle())
                          .append("\n内容：").append(mat.getContent()).append("\n\n");
                    }
                } catch (Exception ignored) {}
            }
        }
        sb.append(prompt);
        return sb.toString();
    }

    // ── 请求体 DTO ────────────────────────────────────────────────

    public static class GenerateRequest {
        @NotBlank(message = "prompt不能为空")
        @Size(max = 8000, message = "prompt不能超过8000字")
        private String prompt;
        private String systemPrompt;
        private Long materialId;
        private java.util.List<Long> materialIds;
        public String getPrompt() { return prompt; }
        public void setPrompt(String prompt) { this.prompt = prompt; }
        public String getSystemPrompt() { return systemPrompt; }
        public void setSystemPrompt(String systemPrompt) { this.systemPrompt = systemPrompt; }
        public Long getMaterialId() { return materialId; }
        public void setMaterialId(Long materialId) { this.materialId = materialId; }
        public java.util.List<Long> getMaterialIds() { return materialIds; }
        public void setMaterialIds(java.util.List<Long> materialIds) { this.materialIds = materialIds; }
    }

    public static class SuggestRequest {
        @NotBlank(message = "标题不能为空")
        private String title;
        @NotBlank(message = "分类不能为空")
        private String category;
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
    }

    public static class PolishRequest {
        @NotBlank(message = "文本不能为空")
        @Size(max = 5000, message = "文本不能超过5000字")
        private String text;
        public String getText() { return text; }
        public void setText(String text) { this.text = text; }
    }
}
