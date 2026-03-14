package com.history.creation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_material")
public class UserMaterial {
    @TableId(type = IdType.AUTO)
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
