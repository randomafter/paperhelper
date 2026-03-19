package com.history.creation.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.history.creation.common.Result;
import com.history.creation.entity.MaterialCategory;
import com.history.creation.mapper.MaterialCategoryMapper;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 素材分类管理接口
 */
@RestController
@RequestMapping("/categories")
public class MaterialCategoryController {

    private final MaterialCategoryMapper categoryMapper;

    public MaterialCategoryController(MaterialCategoryMapper categoryMapper) {
        this.categoryMapper = categoryMapper;
    }

    /** 获取所有分类（所有人可访问）*/
    @GetMapping
    public Result<List<MaterialCategory>> list() {
        List<MaterialCategory> list = categoryMapper.selectList(
                new LambdaQueryWrapper<MaterialCategory>().orderByAsc(MaterialCategory::getSortOrder));
        return Result.ok(list);
    }

    /** 新增分类（仅管理员）*/
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<MaterialCategory> create(@RequestBody MaterialCategory category) {
        // 检查名称重复
        Long count = categoryMapper.selectCount(
                new LambdaQueryWrapper<MaterialCategory>().eq(MaterialCategory::getName, category.getName()));
        if (count > 0) return Result.fail(400, "分类名称已存在");
        if (category.getSortOrder() == null) category.setSortOrder(100);
        categoryMapper.insert(category);
        return Result.ok(category);
    }

    /** 修改分类（仅管理员）*/
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<MaterialCategory> update(@PathVariable Long id, @RequestBody MaterialCategory category) {
        MaterialCategory existing = categoryMapper.selectById(id);
        if (existing == null) return Result.fail(404, "分类不存在");
        // 检查名称重复（排除自身）
        Long count = categoryMapper.selectCount(
                new LambdaQueryWrapper<MaterialCategory>()
                        .eq(MaterialCategory::getName, category.getName())
                        .ne(MaterialCategory::getId, id));
        if (count > 0) return Result.fail(400, "分类名称已存在");
        existing.setName(category.getName());
        if (category.getSortOrder() != null) existing.setSortOrder(category.getSortOrder());
        categoryMapper.updateById(existing);
        return Result.ok(existing);
    }

    /** 删除分类（仅管理员）*/
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<?> delete(@PathVariable Long id) {
        MaterialCategory existing = categoryMapper.selectById(id);
        if (existing == null) return Result.fail(404, "分类不存在");
        categoryMapper.deleteById(id);
        return Result.ok("删除成功");
    }
}
