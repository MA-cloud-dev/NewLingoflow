package com.lingoflow.service;

import com.lingoflow.dto.LoginRequest;
import com.lingoflow.dto.LoginResponse;
import com.lingoflow.dto.RegisterRequest;
import com.lingoflow.entity.User;
import com.lingoflow.exception.BusinessException;
import com.lingoflow.mapper.UserMapper;
import com.lingoflow.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public Map<String, Object> register(RegisterRequest request) {
        // 验证两次密码是否一致
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException(BusinessException.PASSWORD_MISMATCH, "两次密码不一致");
        }

        // 检查用户名是否已存在
        if (userMapper.existsByUsername(request.getUsername())) {
            throw new BusinessException(BusinessException.USERNAME_EXISTS, "用户名已存在");
        }

        // 检查邮箱是否已注册
        if (request.getEmail() != null && !request.getEmail().isEmpty()
                && userMapper.existsByEmail(request.getEmail())) {
            throw new BusinessException(BusinessException.EMAIL_EXISTS, "邮箱已注册");
        }

        // 创建用户
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setDailyGoal(20);
        user.setDifficultyLevel("medium");

        userMapper.insert(user);

        Map<String, Object> result = new HashMap<>();
        result.put("userId", user.getId());
        result.put("username", user.getUsername());
        return result;
    }

    public LoginResponse login(LoginRequest request) {
        // 查找用户
        User user = userMapper.findByUsername(request.getUsername());
        if (user == null) {
            throw new BusinessException(BusinessException.INVALID_CREDENTIALS, "用户名或密码错误");
        }

        // 验证密码
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BusinessException(BusinessException.INVALID_CREDENTIALS, "用户名或密码错误");
        }

        // 生成 Token
        String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getUsername());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId(), user.getUsername());

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(jwtTokenProvider.getAccessExpiration() / 1000)
                .user(LoginResponse.UserInfo.builder()
                        .id(user.getId())
                        .username(user.getUsername())
                        .avatar(user.getAvatarUrl())
                        .build())
                .build();
    }

    public Map<String, Object> refreshToken(String refreshToken) {
        if (!jwtTokenProvider.validateToken(refreshToken) || !jwtTokenProvider.isRefreshToken(refreshToken)) {
            throw new BusinessException(BusinessException.INVALID_REFRESH_TOKEN, "Refresh Token 无效或已过期");
        }

        Long userId = jwtTokenProvider.getUserIdFromToken(refreshToken);
        String username = jwtTokenProvider.getUsernameFromToken(refreshToken);

        String newAccessToken = jwtTokenProvider.generateAccessToken(userId, username);

        Map<String, Object> result = new HashMap<>();
        result.put("accessToken", newAccessToken);
        result.put("expiresIn", jwtTokenProvider.getAccessExpiration() / 1000);
        return result;
    }
}
