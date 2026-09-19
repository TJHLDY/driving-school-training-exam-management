package com.example.drivingschool.controller;

import com.example.drivingschool.common.ApiResponse;
import com.example.drivingschool.common.PageResult;
import com.example.drivingschool.dto.AccountDtos;
import com.example.drivingschool.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AccountController {
    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping("/users")
    public ApiResponse<PageResult<AccountDtos.UserView>> users(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return ApiResponse.ok(accountService.listUsers(keyword, page, pageSize));
    }

    @PostMapping("/users")
    public ApiResponse<AccountDtos.UserView> createUser(@Valid @RequestBody AccountDtos.UserCreateRequest request) {
        return ApiResponse.ok(accountService.createUser(request));
    }

    @PutMapping("/users/{userId}")
    public ApiResponse<AccountDtos.UserView> updateUser(@PathVariable Long userId,
                                                        @Valid @RequestBody AccountDtos.UserUpdateRequest request) {
        return ApiResponse.ok(accountService.updateUser(userId, request));
    }

    @PutMapping("/users/{userId}/roles")
    public ApiResponse<Void> updateRoles(@PathVariable Long userId,
                                         @Valid @RequestBody AccountDtos.UserRoleUpdateRequest request) {
        accountService.updateRoles(userId, request);
        return ApiResponse.ok();
    }

    @PutMapping("/users/{userId}/enabled")
    public ApiResponse<Void> updateEnabled(@PathVariable Long userId,
                                           @Valid @RequestBody AccountDtos.EnableRequest request) {
        accountService.updateEnabled(userId, request);
        return ApiResponse.ok();
    }

    @PutMapping("/users/{userId}/password")
    public ApiResponse<Void> resetPassword(@PathVariable Long userId,
                                           @Valid @RequestBody AccountDtos.ResetPasswordRequest request) {
        accountService.resetPassword(userId, request);
        return ApiResponse.ok();
    }

    @GetMapping("/roles")
    public ApiResponse<List<AccountDtos.RoleView>> roles() {
        return ApiResponse.ok(accountService.listRoles());
    }

    @PostMapping("/roles")
    public ApiResponse<AccountDtos.RoleView> createRole(@Valid @RequestBody AccountDtos.RoleView request) {
        return ApiResponse.ok(accountService.createRole(request));
    }

    @PutMapping("/roles/{roleId}")
    public ApiResponse<AccountDtos.RoleView> updateRole(@PathVariable Long roleId,
                                                        @Valid @RequestBody AccountDtos.RoleView request) {
        return ApiResponse.ok(accountService.updateRole(roleId, request));
    }

    @GetMapping("/menus")
    public ApiResponse<List<AccountDtos.MenuView>> menus() {
        return ApiResponse.ok(accountService.listMenus());
    }

    @PostMapping("/menus")
    public ApiResponse<AccountDtos.MenuView> createMenu(@Valid @RequestBody AccountDtos.MenuSaveRequest request) {
        return ApiResponse.ok(accountService.createMenu(request));
    }

    @PutMapping("/menus/{menuId}")
    public ApiResponse<AccountDtos.MenuView> updateMenu(@PathVariable Long menuId,
                                                        @Valid @RequestBody AccountDtos.MenuSaveRequest request) {
        return ApiResponse.ok(accountService.updateMenu(menuId, request));
    }
}
