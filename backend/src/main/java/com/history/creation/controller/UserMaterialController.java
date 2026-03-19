package com.history.creation.controller;

import com.history.creation.common.Result;
import com.history.creation.dto.UserMaterialDTO;
import com.history.creation.dto.UserMaterialRequest;
import com.history.creation.security.SecurityUtils;
import com.history.creation.service.UserMaterialService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/user-materials")
public class UserMaterialController {

    private final UserMaterialService userMaterialService;

    public UserMaterialController(UserMaterialService userMaterialService) {
        this.userMaterialService = userMaterialService;
    }

    /** 获取当前用户自建素材列表 */
    @GetMapping
    public Result<List<UserMaterialDTO>> listMine() {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.ok(userMaterialService.listByUser(userId));
    }

    /** 创建草稿 */
    @PostMapping
    public Result<UserMaterialDTO> create(@Valid @RequestBody UserMaterialRequest req) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.ok(userMaterialService.create(userId, req));
    }

    /** 编辑素材 */
    @PutMapping("/{id}")
    public Result<UserMaterialDTO> update(@PathVariable Long id, @Valid @RequestBody UserMaterialRequest req) {
        Long userId = SecurityUtils.getCurrentUserId();
        try {
            return Result.ok(userMaterialService.update(userId, id, req));
        } catch (RuntimeException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 提交审核 */
    @PostMapping("/{id}/submit")
    public Result<UserMaterialDTO> submit(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        try {
            return Result.ok(userMaterialService.submit(userId, id));
        } catch (RuntimeException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 删除 */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        try {
            userMaterialService.delete(userId, id);
            return Result.ok(null);
        } catch (RuntimeException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 更新自建素材的收藏分组 */
    @PostMapping("/{id}/group")
    public Result<?> updateGroup(@PathVariable Long id, @RequestParam String groupName) {
        Long userId = SecurityUtils.getCurrentUserId();
        try {
            userMaterialService.updateGroup(userId, id, groupName);
            return Result.ok("更新成功");
        } catch (RuntimeException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 管理员：待审核列表 */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/pending")
    public Result<List<UserMaterialDTO>> listPending() {
        return Result.ok(userMaterialService.listPending());
    }

    /** 管理员：审核通过 */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/{id}/approve")
    public Result<Void> approve(@PathVariable Long id,
                                @RequestBody(required = false) Map<String, String> body) {
        Long adminId = SecurityUtils.getCurrentUserId();
        String comment = body != null ? body.get("comment") : null;
        try {
            userMaterialService.approve(adminId, id, comment);
            return Result.ok(null);
        } catch (RuntimeException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 管理员：审核拒绝 */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/{id}/reject")
    public Result<Void> reject(@PathVariable Long id,
                               @RequestBody(required = false) Map<String, String> body) {
        Long adminId = SecurityUtils.getCurrentUserId();
        String comment = body != null ? body.get("comment") : null;
        try {
            userMaterialService.reject(adminId, id, comment);
            return Result.ok(null);
        } catch (RuntimeException e) {
            return Result.fail(400, e.getMessage());
        }
    }
}
