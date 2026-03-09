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
    private LocalDateTime createdAt;
}
