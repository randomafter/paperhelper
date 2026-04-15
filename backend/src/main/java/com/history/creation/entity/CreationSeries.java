package com.history.creation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("creation_series")
public class CreationSeries {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String groupName;         // 与 creation_work.group_name 关联
    private String pinnedOutline;     // 系列总大纲
    private String charProfiles;      // 系列人物设定文本（兼容旧AI上下文）
    private String outlineData;       // 结构化章节大纲 JSON
    private String charProfilesJson;  // 结构化人物卡 JSON
    private String worldSetting;      // 世界观/设定 JSON
    private String plotHooks;         // 伏笔清单 JSON
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
