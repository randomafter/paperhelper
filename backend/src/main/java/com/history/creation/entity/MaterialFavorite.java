package com.history.creation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("material_favorite")
public class MaterialFavorite {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long materialId;
    /**
     * 用户自定义分组名称，例如：我的灵感/未定义/其他自定义分组
     */
    private String groupName;
    private LocalDateTime createdAt;
}
