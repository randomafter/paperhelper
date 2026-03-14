package com.history.creation.controller;

import com.history.creation.common.Result;
import com.history.creation.entity.Notification;
import com.history.creation.security.SecurityUtils;
import com.history.creation.service.NotificationService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public Result<List<Notification>> listMine() {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.ok(notificationService.listByUser(userId));
    }

    @GetMapping("/unread-count")
    public Result<Long> unreadCount() {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.ok(notificationService.countUnread(userId));
    }

    @PostMapping("/{id}/read")
    public Result<Void> markRead(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        try {
            notificationService.markAsRead(userId, id);
            return Result.ok(null);
        } catch (RuntimeException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    @PostMapping("/read-all")
    public Result<Void> markAllRead() {
        Long userId = SecurityUtils.getCurrentUserId();
        notificationService.markAllRead(userId);
        return Result.ok(null);
    }
}
