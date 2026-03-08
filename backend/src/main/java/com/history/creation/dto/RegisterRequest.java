package com.history.creation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {
    @NotBlank(message = "用户名不能为空")
    @Size(min = 2, max = 64)
    private String username;
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 32)
    private String password;
    private String email;
    private String phone;
    private String nickname;
    private String verificationCode; // 可选，Mock 时可忽略
}
