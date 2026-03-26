package com.history.creation.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CreationSeriesDTO {
    private Long id;
    private Long userId;
    private String groupName;
    private String pinnedOutline;
    private String charProfiles;
    private String charProfilesJson;
    private String worldSetting;
    private String plotHooks;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
