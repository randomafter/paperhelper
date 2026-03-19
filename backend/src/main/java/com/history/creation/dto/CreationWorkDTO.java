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
    private String pinnedOutline;
    private String charProfiles;
    private String outlineData;
    private LocalDateTime lastOpenedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
