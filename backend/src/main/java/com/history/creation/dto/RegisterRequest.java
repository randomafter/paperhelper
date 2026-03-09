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
    private String nickname;
    @NotBlank(message = "安全问题不能为空")
    private String securityQuestion;
    @NotBlank(message = "安全答案不能为空")
    private String securityAnswer;
}
