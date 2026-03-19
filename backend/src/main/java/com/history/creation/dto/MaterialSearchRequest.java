package com.history.creation.dto;

import lombok.Data;

@Data
public class MaterialSearchRequest {
    private String category;
    private String keyword;
    private String tag;
    private String status;
    private Boolean showAll; // 管理端传 true 展示所有状态
    private Integer page = 1;
    private Integer size = 20;
}
