package com.history.creation.controller;

import com.history.creation.common.Result;
import com.history.creation.entity.WorkGroup;
import com.history.creation.security.SecurityUtils;
import com.history.creation.service.WorkGroupService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/work-groups")
public class WorkGroupController {

    private final WorkGroupService workGroupService;

    public WorkGroupController(WorkGroupService workGroupService) {
        this.workGroupService = workGroupService;
    }

    /** 获取我的所有分组 */
    @GetMapping
    public Result<List<WorkGroup>> list() {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.ok(workGroupService.listByUser(userId));
    }

    /** 创建分组 */
    @PostMapping
    public Result<WorkGroup> create(@RequestBody Map<String, String> body) {
        Long userId = SecurityUtils.getCurrentUserId();
        String name = body.get("name");
        try {
            return Result.ok(workGroupService.create(userId, name));
        } catch (RuntimeException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 重命名分组 */
    @PutMapping("/{id}")
    public Result<WorkGroup> rename(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        Long userId = SecurityUtils.getCurrentUserId();
        String newName = body.get("name");
        try {
            return Result.ok(workGroupService.rename(userId, id, newName));
        } catch (RuntimeException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 删除分组 */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        try {
            workGroupService.delete(userId, id);
            return Result.ok(null);
        } catch (RuntimeException e) {
            return Result.fail(400, e.getMessage());
        }
    }
}
