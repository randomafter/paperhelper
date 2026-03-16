package com.history.creation.controller;

import com.history.creation.common.Result;
import com.history.creation.entity.FavoriteGroup;
import com.history.creation.security.SecurityUtils;
import com.history.creation.service.FavoriteGroupService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/favorite-groups")
public class FavoriteGroupController {

    private final FavoriteGroupService favoriteGroupService;

    public FavoriteGroupController(FavoriteGroupService favoriteGroupService) {
        this.favoriteGroupService = favoriteGroupService;
    }

    @GetMapping
    public Result<List<FavoriteGroup>> list() {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.ok(favoriteGroupService.listByUser(userId));
    }

    @PostMapping
    public Result<FavoriteGroup> create(@RequestBody Map<String, String> body) {
        Long userId = SecurityUtils.getCurrentUserId();
        String name = body.get("name");
        try {
            return Result.ok(favoriteGroupService.create(userId, name));
        } catch (RuntimeException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public Result<FavoriteGroup> rename(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        Long userId = SecurityUtils.getCurrentUserId();
        String newName = body.get("name");
        try {
            return Result.ok(favoriteGroupService.rename(userId, id, newName));
        } catch (RuntimeException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        try {
            favoriteGroupService.delete(userId, id);
            return Result.ok(null);
        } catch (RuntimeException e) {
            return Result.fail(400, e.getMessage());
        }
    }
}
