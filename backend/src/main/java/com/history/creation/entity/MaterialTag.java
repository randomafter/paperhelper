package com.history.creation.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("material_tag")
public class MaterialTag {
    private Long materialId;
    private Long tagId;
}
