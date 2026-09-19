package com.example.drivingschool.config;

import com.example.drivingschool.entity.Role;
import com.example.drivingschool.entity.UserAccount;
import com.example.drivingschool.mapper.RoleMapper;
import com.example.drivingschool.mapper.UserAccountMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.List;
import java.util.stream.Collectors;

/** 登录拦截器：会话里没有登录用户时，尝试用 JWT Cookie 恢复，实现七天免登录 */
@Component
public class LoginInterceptor implements HandlerInterceptor {

    public static final String TOKEN_COOKIE = "DS_TOKEN";

    @Autowired private JwtUtil jwtUtil;
    @Autowired private UserAccountMapper userAccountMapper;
    @Autowired private RoleMapper roleMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {
        HttpSession session = request.getSession();
        if (session.getAttribute("loginUser") != null) {
            return true;
        }
        SessionUser fromToken = restoreByToken(request);
        if (fromToken != null) {
            session.setAttribute("loginUser", fromToken);
            return true;
        }
        response.sendRedirect(request.getContextPath() + "/login");
        return false;
    }

    /** 验签 → 查账号 → 重新读角色（角色改过后立即生效） */
    private SessionUser restoreByToken(HttpServletRequest request) {
        String token = readToken(request);
        if (token == null) {
            return null;
        }
        try {
            Claims claims = jwtUtil.parse(token);
            Long userId = Long.valueOf(claims.getSubject());
            UserAccount account = userAccountMapper.selectById(userId);
            if (account == null || Boolean.FALSE.equals(account.getEnabled())) {
                return null;
            }
            List<Role> roles = roleMapper.selectByUserId(userId);
            return new SessionUser(account.getUserId(), account.getUsername(), account.getRealName(),
                    roles.stream().map(Role::getRoleCode).collect(Collectors.toList()),
                    roles.stream().map(Role::getRoleName).collect(Collectors.joining(" / ")));
        } catch (JwtException | IllegalArgumentException e) {
            // 过期、被篡改或格式不对，按未登录处理
            return null;
        }
    }

    private String readToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (TOKEN_COOKIE.equals(cookie.getName()) && cookie.getValue() != null
                    && !cookie.getValue().isBlank()) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
