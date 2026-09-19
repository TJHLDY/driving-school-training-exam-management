package com.example.drivingschool.service;

import com.example.drivingschool.dto.AuthDtos;
import com.example.drivingschool.entity.Menu;
import com.example.drivingschool.entity.UserAccount;
import com.example.drivingschool.exception.BusinessException;
import com.example.drivingschool.mapper.AccountMapper;
import com.example.drivingschool.security.JwtService;
import com.example.drivingschool.security.LoginUser;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AuthService {
    private final AccountMapper accountMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final CurrentUserService currentUserService;

    public AuthService(AccountMapper accountMapper, PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager, JwtService jwtService,
                       CurrentUserService currentUserService) {
        this.accountMapper = accountMapper;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.currentUserService = currentUserService;
    }

    @Transactional
    public AuthDtos.LoginResponse register(AuthDtos.RegisterRequest request) {
        if (accountMapper.findByUsername(request.username()) != null) {
            throw new BusinessException(HttpStatus.CONFLICT, "用户名已存在");
        }
        UserAccount user = new UserAccount();
        user.setUsername(request.username().trim());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRealName(request.realName().trim());
        user.setPhone(request.phone());
        user.setGender(blankToNull(request.gender()));
        user.setIdNumber(blankToNull(request.idNumber()));
        user.setEnabled(true);
        accountMapper.insertUser(user);

        Long studentRoleId = accountMapper.findRoleIdByCode("STUDENT");
        if (studentRoleId == null) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "STUDENT角色未初始化");
        }
        accountMapper.insertUserRole(user.getUserId(), studentRoleId);
        return toLoginResponse(user, List.of("STUDENT"));
    }

    public LoginResult login(AuthDtos.LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password()));
            LoginUser principal = (LoginUser) authentication.getPrincipal();
            UserAccount account = accountMapper.findById(principal.userId());
            if (account == null || !Boolean.TRUE.equals(account.getEnabled())) {
                throw new BusinessException(HttpStatus.UNAUTHORIZED, "账号不可用");
            }
            AuthDtos.LoginResponse response = toLoginResponse(account,
                    accountMapper.findRoleCodesByUserId(account.getUserId()));
            return new LoginResult(jwtService.createToken(principal), response, jwtService.expiresInSeconds());
        } catch (AuthenticationException ex) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "用户名或密码错误");
        }
    }

    public AuthDtos.LoginResponse currentUser() {
        LoginUser principal = currentUserService.requireUser();
        UserAccount account = accountMapper.findById(principal.userId());
        if (account == null || !Boolean.TRUE.equals(account.getEnabled())) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "账号不可用");
        }
        return toLoginResponse(account, accountMapper.findRoleCodesByUserId(account.getUserId()));
    }

    private AuthDtos.LoginResponse toLoginResponse(UserAccount account, List<String> roles) {
        List<AuthDtos.MenuView> menus = accountMapper.findMenusByUserId(account.getUserId()).stream()
                .map(this::toMenuView).toList();
        return new AuthDtos.LoginResponse(String.valueOf(account.getUserId()), account.getUsername(),
                account.getRealName(), roles, menus, jwtService.expiresInSeconds());
    }

    private AuthDtos.MenuView toMenuView(Menu menu) {
        return new AuthDtos.MenuView(menu.getMenuCode(), menu.getMenuName(), menu.getRoutePath(), menu.getSortNo());
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    public record LoginResult(String token, AuthDtos.LoginResponse response, long expiresIn) {
    }
}
