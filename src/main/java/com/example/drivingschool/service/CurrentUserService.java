package com.example.drivingschool.service;

import com.example.drivingschool.exception.BusinessException;
import com.example.drivingschool.security.LoginUser;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Arrays;

@Service
public class CurrentUserService {
    public LoginUser requireUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof LoginUser user)) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "未登录或登录已失效");
        }
        if (!user.enabled()) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "账号已停用");
        }
        return user;
    }

    public Long userId() {
        return requireUser().userId();
    }

    public boolean hasRole(String roleCode) {
        return requireUser().roleCodes().contains(roleCode);
    }

    public boolean hasAnyRole(String... roleCodes) {
        LoginUser user = requireUser();
        return Arrays.stream(roleCodes).anyMatch(user.roleCodes()::contains);
    }

    public void requireAnyRole(String... roleCodes) {
        if (!hasAnyRole(roleCodes)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "无权执行该操作");
        }
    }
}
