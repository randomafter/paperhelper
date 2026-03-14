package com.history.creation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class MaterialUpdateRequest {
    @NotNull(message = "素材ID不能为空")
    private Long id;

    @NotBlank(message = "分类不能为空")
    private String category;

    @NotBlank(message = "标题不能为空")
    private String title;

    @NotBlank(message = "内容不能为空")
    private String content;

    private List<String> tags;
}
