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
    private String dynasty;
    private String category;
    private String title;
    private String content;
    private String sourceUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private List<String> tags;

    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private Boolean isFavorite;
}
