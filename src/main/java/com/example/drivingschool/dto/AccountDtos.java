package com.example.drivingschool.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

public final class AccountDtos {

    private AccountDtos() {
    }

    public record UserCreateRequest(
            @NotBlank @Pattern(regexp = "^[A-Za-z0-9_]{4,40}$", message = "格式不正确") String username,
            @NotBlank @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,72}$", message = "密码强度不足") String password,
            @NotBlank @Size(max = 30) String realName,
            @NotBlank @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确") String phone,
            @Size(max = 10) String gender,
            @Pattern(regexp = "^$|^[0-9Xx]{15,18}$", message = "身份证号格式不正确") String idNumber,
            @Size(max = 40) String coachLicense,
            @NotNull List<String> roleCodes
    ) {
    }

    public record UserUpdateRequest(
            @NotBlank @Size(max = 30) String realName,
            @NotBlank @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确") String phone,
            @Size(max = 10) String gender,
            @Pattern(regexp = "^$|^[0-9Xx]{15,18}$", message = "身份证号格式不正确") String idNumber,
            @Size(max = 40) String coachLicense
    ) {
    }

    public record UserRoleUpdateRequest(@NotNull List<String> roleCodes) {
    }

    public record EnableRequest(@NotNull Boolean enabled) {
    }

    public record ResetPasswordRequest(
            @NotBlank @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,72}$",
                    message = "密码强度不足") String password
    ) {
    }

    public record UserView(String userId, String username, String realName, String phone, String gender,
                           String idNumber, String coachLicense, Boolean enabled, LocalDateTime createdAt,
                           List<String> roles) {
    }

    public record RoleView(String roleId, String roleCode, String roleName) {
    }

    public record MenuSaveRequest(Long menuId, @NotNull Long roleId, @NotBlank @Size(max = 40) String menuCode,
                                  @NotBlank @Size(max = 40) String menuName,
                                  @NotBlank @Size(max = 100) String routePath,
                                  @NotNull Integer sortNo, @NotNull Boolean enabled) {
    }

    public record MenuView(String menuId, Long roleId, String menuCode, String menuName, String routePath,
                           Integer sortNo, Boolean enabled) {
    }
}
