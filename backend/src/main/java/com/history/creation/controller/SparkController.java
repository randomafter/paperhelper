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

import java.util.Map;

/**
 * 讯飞星火大模型接口
 */
@RestController
@RequestMapping("/spark")
@Validated
public class SparkController {

    private final SparkService sparkService;
    private final MaterialService materialService;

    public SparkController(SparkService sparkService, MaterialService materialService) {
        this.sparkService = sparkService;
        this.materialService = materialService;
    }

    /**
     * 通用文本生成
     * POST /api/spark/generate
     * Body: { "prompt": "...", "systemPrompt": "...（可选）" }
     */
    @PostMapping("/generate")
    public Result<Map<String, String>> generate(@RequestBody GenerateRequest req) {
        try {
            String finalPrompt = req.getPrompt();
            // 如果传了素材ID，自动拼入素材内容
            if (req.getMaterialId() != null) {
                try {
                    MaterialDTO mat = materialService.getMaterialById(req.getMaterialId(), null);
                    if (mat != null) {
                        finalPrompt = "【参考素材】\n标题：" + mat.getTitle() +
                                "\n内容：" + mat.getContent() + "\n\n" + finalPrompt;
                    }
                } catch (Exception ignored) {}
            }
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
     * 生成历史素材内容建议
     * POST /api/spark/suggest-material
     * Body: { "title": "...", "category": "..." }
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
     * Body: { "text": "..." }
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

    // ── 请求体 DTO ────────────────────────────────────────────────

    public static class GenerateRequest {
        @NotBlank(message = "prompt不能为空")
        @Size(max = 8000, message = "prompt不能超过8000字")
        private String prompt;
        private String systemPrompt;
        private Long materialId;
        public String getPrompt() { return prompt; }
        public void setPrompt(String prompt) { this.prompt = prompt; }
        public String getSystemPrompt() { return systemPrompt; }
        public void setSystemPrompt(String systemPrompt) { this.systemPrompt = systemPrompt; }
        public Long getMaterialId() { return materialId; }
        public void setMaterialId(Long materialId) { this.materialId = materialId; }
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
