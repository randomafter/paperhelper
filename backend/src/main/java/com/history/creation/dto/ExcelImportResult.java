package com.history.creation.dto;

import lombok.Data;

import java.util.List;

@Data
public class ExcelImportResult {
    private int successCount;
    private int failCount;
    private List<String> errors;
}
