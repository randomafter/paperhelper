package com.history.creation.controller;

import com.history.creation.common.Result;
import com.history.creation.dto.AuthResponse;
import com.history.creation.dto.LoginRequest;
import com.history.creation.dto.RegisterRequest;
import com.history.creation.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public Result<AuthResponse> register(@Valid @RequestBody RegisterRequest req) {
        try {
            AuthResponse resp = userService.register(req);
            return Result.ok(resp);
        } catch (RuntimeException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    @PostMapping("/login")
    public Result<AuthResponse> login(@Valid @RequestBody LoginRequest req) {
        try {
            AuthResponse resp = userService.login(req);
            return Result.ok(resp);
        } catch (RuntimeException e) {
            return Result.fail(401, e.getMessage());
        }
    }

    @GetMapping("/security-question")
    public Result<?> getSecurityQuestion(@RequestParam String username) {
        try {
            String question = userService.getSecurityQuestion(username);
            return Result.ok(question);
        } catch (RuntimeException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    @PostMapping("/verify-answer")
    public Result<?> verifySecurityAnswer(@RequestBody Map<String, String> req) {
        try {
            userService.verifySecurityAnswer(req.get("username"), req.get("answer"));
            return Result.ok("答案正确");
        } catch (RuntimeException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    @PostMapping("/reset-password-by-answer")
    public Result<?> resetPasswordByAnswer(@RequestBody Map<String, String> req) {
        try {
            userService.resetPasswordByAnswer(req.get("username"), req.get("answer"), req.get("newPassword"));
            return Result.ok("密码重置成功");
        } catch (RuntimeException e) {
            return Result.fail(400, e.getMessage());
        }
    }
}
