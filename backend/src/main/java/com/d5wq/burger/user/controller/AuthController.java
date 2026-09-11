package com.d5wq.burger.user.controller;

import com.d5wq.burger.common.response.ApiResponse;
import com.d5wq.burger.security.LoginUser;
import com.d5wq.burger.user.dto.LoginRequest;
import com.d5wq.burger.user.dto.SignUpRequest;
import com.d5wq.burger.user.dto.TokenResponse;
import com.d5wq.burger.user.dto.UserResponse;
import com.d5wq.burger.user.service.AuthService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auth", description = "회원가입 / 로그인(JWT) / 내 정보")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/auth/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<UserResponse> signUp(@Valid @RequestBody SignUpRequest request) {
        return ApiResponse.success(authService.signUp(request));
    }

    @PostMapping("/auth/login")
    public ApiResponse<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(authService.login(request));
    }

    @GetMapping("/users/me")
    public ApiResponse<UserResponse> me(@LoginUser Long userId) {
        return ApiResponse.success(authService.getMe(userId));
    }
}
