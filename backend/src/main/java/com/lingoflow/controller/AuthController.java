package com.lingoflow.controller;

import com.lingoflow.dto.ApiResponse;
import com.lingoflow.dto.LoginRequest;
import com.lingoflow.dto.LoginResponse;
import com.lingoflow.dto.RegisterRequest;
import com.lingoflow.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Map<String, Object>>> register(@Valid @RequestBody RegisterRequest request) {
        Map<String, Object> result = authService.register(request);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse result = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<Map<String, Object>>> refresh(@RequestHeader("Authorization") String authHeader) {
        String refreshToken = null;
        if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
            refreshToken = authHeader.substring(7);
        }
        Map<String, Object> result = authService.refreshToken(refreshToken);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout() {
        // 简单实现：客户端清除 Token 即可
        // TODO: 后续可添加 Redis 黑名单
        return ResponseEntity.ok(ApiResponse.success());
    }
}
