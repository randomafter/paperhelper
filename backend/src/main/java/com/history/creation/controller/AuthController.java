package com.history.creation.controller;

import com.history.creation.common.Result;
import com.history.creation.dto.AuthResponse;
import com.history.creation.dto.LoginRequest;
import com.history.creation.dto.RegisterRequest;
import com.history.creation.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
