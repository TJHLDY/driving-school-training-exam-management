package com.example.drivingschool.service;

import com.example.drivingschool.common.PageResult;
import com.example.drivingschool.common.PageUtils;
import com.example.drivingschool.dto.AccountDtos;
import com.example.drivingschool.entity.Menu;
import com.example.drivingschool.entity.Role;
import com.example.drivingschool.entity.UserAccount;
import com.example.drivingschool.exception.BusinessException;
import com.example.drivingschool.mapper.AccountMapper;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class AccountService {
    private final AccountMapper accountMapper;
    private final CurrentUserService currentUserService;
    private final PasswordEncoder passwordEncoder;

    public AccountService(AccountMapper accountMapper, CurrentUserService currentUserService,
                          PasswordEncoder passwordEncoder) {
        this.accountMapper = accountMapper;
        this.currentUserService = currentUserService;
        this.passwordEncoder = passwordEncoder;
    }

    public PageResult<AccountDtos.UserView> listUsers(String keyword, Integer pageNo, Integer pageSize) {
        currentUserService.requireAnyRole("ADMIN");
        int page = PageUtils.page(pageNo);
        int size = PageUtils.pageSize(pageSize);
        long offset = PageUtils.offset(page, size);
        List<AccountDtos.UserView> users = accountMapper.findUsers(keyword, offset, size).stream()
                .map(this::toView).toList();
        return new PageResult<>(users, accountMapper.countUsers(keyword), page, size);
    }

    @Transactional
    public AccountDtos.UserView createUser(AccountDtos.UserCreateRequest request) {
        currentUserService.requireAnyRole("ADMIN");
        if (accountMapper.findByUsername(request.username()) != null) {
            throw new BusinessException(HttpStatus.CONFLICT, "用户名已存在");
        }
        List<Long> roleIds = resolveRoleIds(request.roleCodes());
        UserAccount user = new UserAccount();
        user.setUsername(request.username().trim());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRealName(request.realName().trim());
        user.setPhone(request.phone());
        user.setGender(blankToNull(request.gender()));
        user.setIdNumber(blankToNull(request.idNumber()));
        user.setCoachLicense(blankToNull(request.coachLicense()));
        user.setEnabled(true);
        accountMapper.insertUser(user);
        roleIds.forEach(roleId -> accountMapper.insertUserRole(user.getUserId(), roleId));
        return toView(user);
    }

    @Transactional
    public AccountDtos.UserView updateUser(Long userId, AccountDtos.UserUpdateRequest request) {
        currentUserService.requireAnyRole("ADMIN");
        UserAccount user = requireUser(userId);
        user.setRealName(request.realName().trim());
        user.setPhone(request.phone());
        user.setGender(blankToNull(request.gender()));
        user.setIdNumber(blankToNull(request.idNumber()));
        user.setCoachLicense(blankToNull(request.coachLicense()));
        if (accountMapper.updateUser(user) != 1) {
            throw new BusinessException(HttpStatus.CONFLICT, "用户更新失败");
        }
        return toView(user);
    }

    @Transactional
    public void updateRoles(Long userId, AccountDtos.UserRoleUpdateRequest request) {
        Long actorId = currentUserService.userId();
        currentUserService.requireAnyRole("ADMIN");
        requireUser(userId);
        List<String> existingRoles = accountMapper.findRoleCodesByUserId(userId);
        List<Long> roleIds = resolveRoleIds(request.roleCodes());
        Set<String> newRoleCodes = normalizeRoleCodes(request.roleCodes());
        if (existingRoles.contains("ADMIN") && !newRoleCodes.contains("ADMIN")) {
            if (accountMapper.countUsersByRoleCode("ADMIN") <= 1) {
                throw new BusinessException(HttpStatus.CONFLICT, "不能移除最后一名管理员");
            }
            if (userId.equals(actorId)) {
                throw new BusinessException(HttpStatus.CONFLICT, "不能移除自己的管理员角色");
            }
        }
        accountMapper.deleteUserRoles(userId);
        roleIds.forEach(roleId -> accountMapper.insertUserRole(userId, roleId));
    }

    @Transactional
    public void updateEnabled(Long userId, AccountDtos.EnableRequest request) {
        Long actorId = currentUserService.userId();
        currentUserService.requireAnyRole("ADMIN");
        requireUser(userId);
        if (userId.equals(actorId) && !request.enabled()) {
            throw new BusinessException(HttpStatus.CONFLICT, "不能停用当前账号");
        }
        accountMapper.updateUserEnabled(userId, request.enabled());
    }

    @Transactional
    public void resetPassword(Long userId, AccountDtos.ResetPasswordRequest request) {
        currentUserService.requireAnyRole("ADMIN");
        requireUser(userId);
        accountMapper.updatePassword(userId, passwordEncoder.encode(request.password()));
    }

    public List<AccountDtos.RoleView> listRoles() {
        currentUserService.requireAnyRole("ADMIN");
        return accountMapper.findRoles().stream().map(this::toRoleView).toList();
    }

    @Transactional
    public AccountDtos.RoleView createRole(AccountDtos.RoleView request) {
        currentUserService.requireAnyRole("ADMIN");
        if (request.roleCode() == null || request.roleCode().isBlank()
                || request.roleName() == null || request.roleName().isBlank()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "角色编码和名称不能为空");
        }
        Role role = new Role();
        role.setRoleCode(request.roleCode().trim().toUpperCase(Locale.ROOT));
        role.setRoleName(request.roleName().trim());
        accountMapper.insertRole(role);
        return toRoleView(role);
    }

    @Transactional
    public AccountDtos.RoleView updateRole(Long roleId, AccountDtos.RoleView request) {
        currentUserService.requireAnyRole("ADMIN");
        Role role = new Role();
        role.setRoleId(roleId);
        role.setRoleCode(request.roleCode().trim().toUpperCase(Locale.ROOT));
        role.setRoleName(request.roleName().trim());
        if (accountMapper.updateRole(role) != 1) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "角色不存在");
        }
        return toRoleView(role);
    }

    public List<AccountDtos.MenuView> listMenus() {
        currentUserService.requireAnyRole("ADMIN");
        return accountMapper.findMenus().stream().map(this::toMenuView).toList();
    }

    @Transactional
    public AccountDtos.MenuView createMenu(AccountDtos.MenuSaveRequest request) {
        currentUserService.requireAnyRole("ADMIN");
        Menu menu = fromMenuRequest(request);
        accountMapper.insertMenu(menu);
        return toMenuView(menu);
    }

    @Transactional
    public AccountDtos.MenuView updateMenu(Long menuId, AccountDtos.MenuSaveRequest request) {
        currentUserService.requireAnyRole("ADMIN");
        if (accountMapper.findMenuById(menuId) == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "菜单不存在");
        }
        Menu menu = fromMenuRequest(request);
        menu.setMenuId(menuId);
        accountMapper.updateMenu(menu);
        return toMenuView(menu);
    }

    private UserAccount requireUser(Long userId) {
        UserAccount user = accountMapper.findById(userId);
        if (user == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "用户不存在");
        }
        return user;
    }

    private List<Long> resolveRoleIds(List<String> roleCodes) {
        Set<String> codes = normalizeRoleCodes(roleCodes);
        if (codes.isEmpty()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "至少选择一个角色");
        }
        return codes.stream().map(code -> {
            Long id = accountMapper.findRoleIdByCode(code);
            if (id == null) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "角色不存在：" + code);
            }
            return id;
        }).toList();
    }

    private Set<String> normalizeRoleCodes(List<String> roleCodes) {
        Set<String> result = new LinkedHashSet<>();
        if (roleCodes != null) {
            roleCodes.stream().filter(code -> code != null && !code.isBlank())
                    .map(code -> code.trim().toUpperCase(Locale.ROOT)).forEach(result::add);
        }
        return result;
    }

    private AccountDtos.UserView toView(UserAccount user) {
        return new AccountDtos.UserView(String.valueOf(user.getUserId()), user.getUsername(), user.getRealName(),
                user.getPhone(), user.getGender(), user.getIdNumber(), user.getCoachLicense(), user.getEnabled(),
                user.getCreatedAt(), accountMapper.findRoleCodesByUserId(user.getUserId()));
    }

    private AccountDtos.RoleView toRoleView(Role role) {
        return new AccountDtos.RoleView(String.valueOf(role.getRoleId()), role.getRoleCode(), role.getRoleName());
    }

    private AccountDtos.MenuView toMenuView(Menu menu) {
        return new AccountDtos.MenuView(String.valueOf(menu.getMenuId()), menu.getRoleId(), menu.getMenuCode(),
                menu.getMenuName(), menu.getRoutePath(), menu.getSortNo(), menu.getEnabled());
    }

    private Menu fromMenuRequest(AccountDtos.MenuSaveRequest request) {
        Menu menu = new Menu();
        menu.setRoleId(request.roleId());
        menu.setMenuCode(request.menuCode().trim());
        menu.setMenuName(request.menuName().trim());
        menu.setRoutePath(request.routePath().trim());
        menu.setSortNo(request.sortNo());
        menu.setEnabled(request.enabled());
        return menu;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
