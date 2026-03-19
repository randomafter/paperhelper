package com.history.creation.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.history.creation.common.Result;
import com.history.creation.dto.CreationWorkDTO;
import com.history.creation.security.SecurityUtils;
import com.history.creation.service.CreationWorkService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/works")
public class CreationWorkController {

    private final CreationWorkService workService;

    public CreationWorkController(CreationWorkService workService) {
        this.workService = workService;
    }

    @GetMapping
    public Result<Page<CreationWorkDTO>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.ok(workService.listByUser(userId, page, size));
    }

    @GetMapping("/{id}")
    public Result<CreationWorkDTO> get(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        try {
            return Result.ok(workService.getByIdAndUser(id, userId));
        } catch (RuntimeException e) {
            return Result.fail(404, e.getMessage());
        }
    }

    @PostMapping
    public Result<CreationWorkDTO> create(@RequestBody(required = false) Map<String, String> body) {
        Long userId = SecurityUtils.getCurrentUserId();
        String title = body != null ? body.getOrDefault("title", "未命名") : "未命名";
        String content = body != null ? body.getOrDefault("content", "") : "";
        String groupName = body != null ? body.get("groupName") : null;
        return Result.ok(workService.create(userId, title, content, groupName));
    }

    @PutMapping("/{id}")
    public Result<CreationWorkDTO> save(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        Long userId = SecurityUtils.getCurrentUserId();
        String title = body.getOrDefault("title", "未命名");
        String content = body.getOrDefault("content", "");
        String pinnedOutline = body.get("pinnedOutline");
        String charProfiles = body.get("charProfiles");
        String outlineData = body.get("outlineData");
        try {
            return Result.ok(workService.save(userId, id, title, content, pinnedOutline, charProfiles, outlineData));
        } catch (RuntimeException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    @PutMapping("/{id}/group")
    public Result<CreationWorkDTO> updateGroup(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        Long userId = SecurityUtils.getCurrentUserId();
        String groupName = body.get("groupName");
        try {
            return Result.ok(workService.updateGroup(userId, id, groupName));
        } catch (RuntimeException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        try {
            workService.delete(userId, id);
            return Result.ok(null);
        } catch (RuntimeException e) {
            return Result.fail(400, e.getMessage());
        }
    }
}
