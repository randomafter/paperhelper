package com.history.creation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("creation_work")
public class CreationWork {
    @TableId(type = IdType.AUTO)
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
