package com.history.creation.service;

import com.history.creation.util.SparkApiUtil;
import com.history.creation.util.SparkApiUtil.SparkApiException;
import org.springframework.stereotype.Service;

/**
 * 讯飞星火大模型业务服务
 */
@Service
public class SparkService {

    private final SparkApiUtil sparkApiUtil;

    public SparkService(SparkApiUtil sparkApiUtil) {
        this.sparkApiUtil = sparkApiUtil;
    }

    /**
     * 通用文本生成
     */
    public String generate(String prompt) throws SparkApiException {
        return sparkApiUtil.chat(null, prompt);
    }

    /**
     * 带系统指令的文本生成
     */
    public String generateWithSystem(String systemPrompt, String userMessage) throws SparkApiException {
        return sparkApiUtil.chat(systemPrompt, userMessage);
    }

    /**
     * 根据标题生成历史素材内容建议
     */
    public String suggestMaterial(String title, String category) throws SparkApiException {
        String system = "你是一位专注于中国历史文化的内容创作助手。请根据用户提供的素材标题和分类，生成一段简洁、准确、有文学价值的历史素材内容，字数在100-300字之间。";
        String user = String.format("素材标题：%s\n分类：%s\n请生成对应的历史素材内容。", title, category);
        return sparkApiUtil.chat(system, user);
    }

    /**
     * 为创作文章提供润色建议
     */
    public String polishText(String text) throws SparkApiException {
        String system = "你是一位专业的历史文学编辑，擅长古风文学创作。请对用户提供的文章进行润色，保持原意的同时提升文学性和流畅度，直接返回润色后的文本。";
        return sparkApiUtil.chat(system, text);
    }
}
