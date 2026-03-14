package com.history.creation.dto;

import lombok.Data;

@Data
public class MaterialSearchRequest {
    private String category;
    private String keyword;
    private String tag;
    private Integer page = 1;
    private Integer size = 20;
}
