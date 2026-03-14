package com.history.creation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("material")
public class Material {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String category;
    private String title;
    private String content;
    /**
     * 若为用户投稿素材，记录提交人ID；系统预置素材则为null
     */
    private Long sourceUserId;
    /**
     * PUBLISHED=正式; PENDING=待审核; REJECTED=已驳回
     */
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private List<String> tags;

    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private Boolean isFavorite;
}
