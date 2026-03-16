package com.history.creation.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CreationWorkDTO {
    private Long id;
    private Long userId;
    private String title;
    private String content;
    private String groupName;
    private LocalDateTime lastOpenedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
