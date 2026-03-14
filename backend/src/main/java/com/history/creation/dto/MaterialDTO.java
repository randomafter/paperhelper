package com.history.creation.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class MaterialDTO {
    private Long id;
    private String category;
    private String title;
    private String content;
    private Long sourceUserId;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<String> tags;
    private Boolean isFavorite;
    /**
     * 当前登录用户在收藏时所归属的分组名称（仅在收藏列表/工作台侧栏使用）
     */
    private String favoriteGroup;
}
