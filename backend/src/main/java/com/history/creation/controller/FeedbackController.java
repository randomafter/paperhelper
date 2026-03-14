package com.history.creation.controller;

import com.history.creation.common.Result;
import com.history.creation.dto.FeedbackRequest;
import com.history.creation.entity.Feedback;
import com.history.creation.service.FeedbackService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/feedback")
public class FeedbackController {

    private final FeedbackService feedbackService;

    public FeedbackController(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    @PostMapping("/submit")
    public Result<Void> submitFeedback(Authentication auth, @RequestBody FeedbackRequest req) {
        Long userId = (Long) auth.getPrincipal();
        String username = auth.getName();
        try {
            feedbackService.submitFeedback(userId, username, req);
            return Result.ok(null);
        } catch (RuntimeException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    @GetMapping("/all")
    public Result<List<Feedback>> getAllFeedback(Authentication auth) {
        try {
            String role = auth.getAuthorities().stream()
                    .findFirst()
                    .map(a -> a.getAuthority().replace("ROLE_", ""))
                    .orElse("");
            if (!"ADMIN".equals(role)) {
                return Result.fail(403, "仅管理员可查看");
            }
            List<Feedback> feedbacks = feedbackService.getAllFeedback();
            return Result.ok(feedbacks);
        } catch (RuntimeException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    @GetMapping("/status/{status}")
    public Result<List<Feedback>> getFeedbackByStatus(@PathVariable String status, Authentication auth) {
        try {
            String role = auth.getAuthorities().stream()
                    .findFirst()
                    .map(a -> a.getAuthority().replace("ROLE_", ""))
                    .orElse("");
            if (!"ADMIN".equals(role)) {
                return Result.fail(403, "仅管理员可查看");
            }
            List<Feedback> feedbacks = feedbackService.getFeedbackByStatus(status);
            return Result.ok(feedbacks);
        } catch (RuntimeException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    @PostMapping("/{id}/read")
    public Result<Void> markAsRead(@PathVariable Long id, Authentication auth) {
        try {
            String role = auth.getAuthorities().stream()
                    .findFirst()
                    .map(a -> a.getAuthority().replace("ROLE_", ""))
                    .orElse("");
            if (!"ADMIN".equals(role)) {
                return Result.fail(403, "仅管理员可操作");
            }
            feedbackService.markAsRead(id);
            return Result.ok(null);
        } catch (RuntimeException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    @PostMapping("/{id}/resolved")
    public Result<Void> markAsResolved(@PathVariable Long id, Authentication auth) {
        try {
            String role = auth.getAuthorities().stream()
                    .findFirst()
                    .map(a -> a.getAuthority().replace("ROLE_", ""))
                    .orElse("");
            if (!"ADMIN".equals(role)) {
                return Result.fail(403, "仅管理员可操作");
            }
            feedbackService.markAsResolved(id);
            return Result.ok(null);
        } catch (RuntimeException e) {
            return Result.fail(400, e.getMessage());
        }
    }
}
