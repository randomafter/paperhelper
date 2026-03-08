package com.history.creation.controller;

import com.history.creation.common.Result;
import com.history.creation.dto.UserProfileDTO;
import com.history.creation.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public Result<UserProfileDTO> me(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        UserProfileDTO profile = userService.getProfile(userId);
        return Result.ok(profile);
    }

    @PutMapping("/me")
    public Result<UserProfileDTO> updateMe(Authentication auth,
                                           @RequestParam(required = false) String nickname,
                                           @RequestParam(required = false) String email,
                                           @RequestParam(required = false) String phone,
                                           @RequestParam(required = false) String avatar) {
        Long userId = (Long) auth.getPrincipal();
        try {
            UserProfileDTO updated = userService.updateProfile(userId, nickname, email, phone, avatar);
            return Result.ok(updated);
        } catch (RuntimeException e) {
            return Result.fail(400, e.getMessage());
        }
    }
}
