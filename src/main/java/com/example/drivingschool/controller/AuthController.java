package com.example.drivingschool.controller;
import com.example.drivingschool.config.SessionUser;
import com.example.drivingschool.config.JwtUtil;
import com.example.drivingschool.config.LoginInterceptor;
import com.example.drivingschool.entity.Role;
import com.example.drivingschool.entity.UserAccount;
import com.example.drivingschool.mapper.RoleMapper;
import com.example.drivingschool.mapper.UserAccountMapper;
import com.example.drivingschool.mapper.UserRoleMapper;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.List;
import java.util.stream.Collectors;
@Controller

public class AuthController {
    @Autowired private UserAccountMapper userAccountMapper;
    @Autowired private RoleMapper roleMapper;
    @Autowired private UserRoleMapper userRoleMapper;
    @Autowired private BCryptPasswordEncoder passwordEncoder;
    @Autowired private JwtUtil jwtUtil;
    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }
    @PostMapping("/login")
    public String login(@RequestParam String username, @RequestParam String password,
                        HttpSession session, HttpServletResponse response, Model model) {
        UserAccount account = userAccountMapper.selectByUsername(username);
        if (account == null || !passwordEncoder.matches(password, account.getPasswordHash())) {
            model.addAttribute("error", "用户名或密码错误");
            return "login";
        }
        if (Boolean.FALSE.equals(account.getEnabled())) {
            model.addAttribute("error", "账号已停用，请联系管理员");
            return "login";
        }
        List<Role> roles = roleMapper.selectByUserId(account.getUserId());
        List<String> codes = roles.stream().map(Role::getRoleCode).collect(Collectors.toList());
        String names = roles.stream().map(Role::getRoleName).collect(Collectors.joining(" / "));
        session.setAttribute("loginUser", new SessionUser(account.getUserId(), account.getUsername(),
                account.getRealName(), codes, names));

        // 签发 JWT 放进 HttpOnly Cookie，有效期七天，实现七天免登录
        String token = jwtUtil.generate(account.getUserId(), account.getUsername());
        Cookie cookie = new Cookie(LoginInterceptor.TOKEN_COOKIE, token);
        cookie.setHttpOnly(true);          // 前端脚本读不到，防止 XSS 偷令牌
        cookie.setPath("/");
        cookie.setMaxAge((int) (jwtUtil.getExpireMillis() / 1000));
        cookie.setAttribute("SameSite", "Lax");   // 跨站请求不携带，缓解 CSRF
        response.addCookie(cookie);
        return "redirect:/";
    }
    @GetMapping("/register")
    public String registerPage() {
        return "register";
    }
    // 公开注册只能得到 STUDENT 角色
    @PostMapping("/register")
    public String register(@RequestParam String username, @RequestParam String password,
                           @RequestParam String realName, @RequestParam String phone,
                           @RequestParam(required = false) String gender,
                           @RequestParam String idNumber, Model model) {
        if (username.isBlank() || password.isBlank() || realName.isBlank()
                || phone.isBlank() || idNumber.isBlank()) {
            model.addAttribute("error", "用户名、密码、姓名、手机号、身份证号都不能为空");
            return "register";
        }
        if (userAccountMapper.countByUsername(username) > 0) {
            model.addAttribute("error", "用户名已存在");
            return "register";
        }
        if (userAccountMapper.countByPhone(phone) > 0) {
            model.addAttribute("error", "手机号已被注册");
            return "register";
        }
        if (userAccountMapper.countByIdNumber(idNumber) > 0) {
            model.addAttribute("error", "身份证号已被注册");
            return "register";
        }
        UserAccount account = new UserAccount();
        account.setUsername(username);
        account.setPasswordHash(passwordEncoder.encode(password));
        account.setRealName(realName);
        account.setPhone(phone);
        account.setGender(gender);
        account.setIdNumber(idNumber);
        account.setEnabled(true);
        userAccountMapper.insert(account);
        Role studentRole = roleMapper.selectByCode("STUDENT");
        userRoleMapper.insert(account.getUserId(), studentRole.getRoleId());
        model.addAttribute("success", "注册成功，请登录");
        return "login";
    }
    @GetMapping("/logout")
    public String logout(HttpSession session, HttpServletResponse response) {
        session.invalidate();
        // 清掉浏览器里的令牌 Cookie
        Cookie cookie = new Cookie(LoginInterceptor.TOKEN_COOKIE, "");
        cookie.setPath("/");
        cookie.setMaxAge(0);
        cookie.setHttpOnly(true);
        response.addCookie(cookie);
        return "redirect:/login";
    }
}
