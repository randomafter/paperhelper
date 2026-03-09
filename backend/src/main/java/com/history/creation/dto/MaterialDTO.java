package com.history.creation.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class MaterialDTO {
    private Long id;
    private String dynasty;
    private String category;
    private String title;
    private String content;
    private String sourceUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<String> tags;
    private Boolean isFavorite;
}
