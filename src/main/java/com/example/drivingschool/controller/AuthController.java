package com.example.drivingschool.controller;

import com.example.drivingschool.common.ApiResponse;
import com.example.drivingschool.config.JwtProperties;
import com.example.drivingschool.dto.AuthDtos;
import com.example.drivingschool.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;
    private final JwtProperties jwtProperties;

    public AuthController(AuthService authService, JwtProperties jwtProperties) {
        this.authService = authService;
        this.jwtProperties = jwtProperties;
    }

    @GetMapping("/csrf")
    public ApiResponse<Map<String, String>> csrf(CsrfToken token) {
        Map<String, String> data = new LinkedHashMap<>();
        data.put("token", token.getToken());
        data.put("headerName", token.getHeaderName());
        data.put("parameterName", token.getParameterName());
        return ApiResponse.ok(data);
    }

    @PostMapping("/register")
    public ApiResponse<AuthDtos.LoginResponse> register(@Valid @RequestBody AuthDtos.RegisterRequest request) {
        return ApiResponse.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthDtos.LoginResponse>> login(
            @Valid @RequestBody AuthDtos.LoginRequest request) {
        AuthService.LoginResult result = authService.login(request);
        ResponseCookie cookie = ResponseCookie.from(jwtProperties.cookieName(), result.token())
                .httpOnly(true).secure(jwtProperties.cookieSecure()).sameSite(jwtProperties.cookieSameSite())
                .path("/").maxAge(Duration.ofSeconds(result.expiresIn())).build();
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(ApiResponse.ok(result.response()));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout() {
        SecurityContextHolder.clearContext();
        ResponseCookie cookie = ResponseCookie.from(jwtProperties.cookieName(), "")
                .httpOnly(true).secure(jwtProperties.cookieSecure()).sameSite(jwtProperties.cookieSameSite())
                .path("/").maxAge(Duration.ZERO).build();
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, cookie.toString()).body(ApiResponse.ok());
    }

    @GetMapping("/me")
    public ApiResponse<AuthDtos.LoginResponse> me() {
        return ApiResponse.ok(authService.currentUser());
    }
}
