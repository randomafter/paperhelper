package com.history.creation.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserMaterialDTO {
    private Long id;
    private Long userId;
    private String category;
    private String title;
    private String content;
    private String tags;
    /** draft / pending / approved / rejected */
    private String status;
    private String adminComment;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
