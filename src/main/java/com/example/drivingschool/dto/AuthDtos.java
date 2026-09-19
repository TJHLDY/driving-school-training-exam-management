package com.example.drivingschool.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record RegisterRequest(
            @NotBlank @Pattern(regexp = "^[A-Za-z0-9_]{4,40}$", message = "必须为4-40位字母、数字或下划线") String username,
            @NotBlank @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,72}$",
                    message = "必须为8-72位且包含大小写字母、数字和特殊字符") String password,
            @NotBlank @Size(max = 30) String realName,
            @NotBlank @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确") String phone,
            @Size(max = 10) String gender,
            @Pattern(regexp = "^$|^[0-9Xx]{15,18}$", message = "身份证号格式不正确") String idNumber
    ) {
    }

    public record LoginRequest(
            @NotBlank String username,
            @NotBlank String password
    ) {
    }

    public record LoginResponse(
            String userId,
            String username,
            String realName,
            List<String> roles,
            List<MenuView> menus,
            long expiresIn
    ) {
    }

    public record MenuView(String menuCode, String menuName, String routePath, Integer sortNo) {
    }
}
