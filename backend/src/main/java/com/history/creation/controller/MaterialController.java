package com.history.creation.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.history.creation.common.Result;
import com.history.creation.dto.ExcelImportResult;
import com.history.creation.dto.MaterialCreateRequest;
import com.history.creation.dto.MaterialDTO;
import com.history.creation.dto.MaterialSearchRequest;
import com.history.creation.dto.MaterialUpdateRequest;
import com.history.creation.security.SecurityUtils;
import com.history.creation.service.MaterialService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/materials")
public class MaterialController {

    private final MaterialService materialService;

    public MaterialController(MaterialService materialService) {
        this.materialService = materialService;
    }

    @GetMapping
    public Result<Page<MaterialDTO>> searchMaterials(MaterialSearchRequest req) {
        Long userId = SecurityUtils.getCurrentUserId();
        Page<MaterialDTO> result = materialService.searchMaterials(req, userId);
        return Result.ok(result);
    }

    @GetMapping("/{id}")
    public Result<MaterialDTO> getMaterial(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        MaterialDTO result = materialService.getMaterialById(id, userId);
        return Result.ok(result);
    }

    @GetMapping("/tags")
    public Result<List<String>> getAllTags() {
        List<String> tags = materialService.getAllTags();
        return Result.ok(tags);
    }

    @PostMapping
    public Result<MaterialDTO> createMaterial(@Valid @RequestBody MaterialCreateRequest req) {
        try {
            Long userId = SecurityUtils.getCurrentUserId();
            // 判断是否管理员，管理员创建直接发布
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            boolean isAdmin = auth != null && auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
            MaterialDTO result = materialService.createMaterial(req, isAdmin ? null : userId);
            return Result.ok(result);
        } catch (RuntimeException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<MaterialDTO> updateMaterial(@Valid @RequestBody MaterialUpdateRequest req) {
        try {
            MaterialDTO result = materialService.updateMaterial(req);
            return Result.ok(result);
        } catch (RuntimeException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<?> deleteMaterial(@PathVariable Long id) {
        try {
            materialService.deleteMaterial(id);
            return Result.ok("删除成功");
        } catch (RuntimeException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    @PostMapping("/{id}/favorite")
    public Result<?> toggleFavorite(@PathVariable Long id) {
        try {
            Long userId = SecurityUtils.getCurrentUserId();
            materialService.toggleFavorite(userId, id);
            return Result.ok("操作成功");
        } catch (RuntimeException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    @GetMapping("/favorites")
    public Result<List<MaterialDTO>> getFavorites() {
        Long userId = SecurityUtils.getCurrentUserId();
        List<MaterialDTO> result = materialService.getFavorites(userId);
        return Result.ok(result);
    }

    @PostMapping("/{id}/favorite/group")
    public Result<?> updateFavoriteGroup(@PathVariable Long id, @RequestParam String groupName) {
        try {
            Long userId = SecurityUtils.getCurrentUserId();
            materialService.updateFavoriteGroup(userId, id, groupName);
            return Result.ok("更新成功");
        } catch (RuntimeException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    @PostMapping("/import")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<ExcelImportResult> importMaterials(@RequestParam("file") MultipartFile file) {
        try {
            ExcelImportResult result = materialService.importFromExcel(file);
            return Result.ok(result);
        } catch (Exception e) {
            return Result.fail(400, "导入失败：" + e.getMessage());
        }
    }
}
