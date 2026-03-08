package com.history.creation.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.history.creation.dto.AuthResponse;
import com.history.creation.dto.LoginRequest;
import com.history.creation.dto.RegisterRequest;
import com.history.creation.dto.UserProfileDTO;
import com.history.creation.entity.Role;
import com.history.creation.entity.User;
import com.history.creation.mapper.RoleMapper;
import com.history.creation.mapper.UserMapper;
import com.history.creation.security.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class UserService {

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public UserService(UserMapper userMapper, RoleMapper roleMapper,
                       PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        if (userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, req.getUsername())) != null) {
            throw new RuntimeException("用户名已存在");
        }
        if (req.getEmail() != null && !req.getEmail().isBlank()
                && userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getEmail, req.getEmail())) != null) {
            throw new RuntimeException("邮箱已被注册");
        }
        User user = new User();
        user.setUsername(req.getUsername());
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setEmail(req.getEmail());
        user.setPhone(req.getPhone());
        user.setNickname(req.getNickname() != null ? req.getNickname() : req.getUsername());
        user.setRoleId(2L); // CREATOR
        user.setEnabled(true);
        userMapper.insert(user);
        Role role = roleMapper.selectById(2L);
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), role.getName());
        return new AuthResponse(token, user.getId(), user.getUsername(), role.getName(), user.getNickname());
    }

    public AuthResponse login(LoginRequest req) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, req.getUsername()));
        if (user == null || !passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new RuntimeException("用户名或密码错误");
        }
        if (!Boolean.TRUE.equals(user.getEnabled())) {
            throw new RuntimeException("账号已禁用");
        }
        Role role = roleMapper.selectById(user.getRoleId());
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), role.getName());
        return new AuthResponse(token, user.getId(), user.getUsername(), role.getName(),
                Optional.ofNullable(user.getNickname()).orElse(user.getUsername()));
    }

    public UserProfileDTO getProfile(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) return null;
        Role role = roleMapper.selectById(user.getRoleId());
        UserProfileDTO dto = new UserProfileDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setPhone(user.getPhone());
        dto.setNickname(user.getNickname());
        dto.setAvatar(user.getAvatar());
        dto.setRole(role != null ? role.getName() : null);
        return dto;
    }

    @Transactional
    public UserProfileDTO updateProfile(Long userId, String nickname, String email, String phone, String avatar) {
        User user = userMapper.selectById(userId);
        if (user == null) throw new RuntimeException("用户不存在");
        if (nickname != null) user.setNickname(nickname);
        if (email != null) user.setEmail(email);
        if (phone != null) user.setPhone(phone);
        if (avatar != null) user.setAvatar(avatar);
        userMapper.updateById(user);
        return getProfile(userId);
    }
}
