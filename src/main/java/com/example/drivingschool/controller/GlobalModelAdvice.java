package com.example.drivingschool.controller;
import com.example.drivingschool.config.SessionUser;
import com.example.drivingschool.mapper.MenuMapper;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import java.util.Collections;
// 每个页面都能拿到当前登录用户和它有权看到的菜单
@ControllerAdvice

public class GlobalModelAdvice {
    @Autowired
    private MenuMapper menuMapper;
    @ModelAttribute
    public void addCommon(Model model, HttpSession session) {
        SessionUser user = (SessionUser) session.getAttribute("loginUser");
        model.addAttribute("loginUser", user);
        model.addAttribute("menus", user == null
                ? Collections.emptyList()
                : menuMapper.selectByUserId(user.getUserId()));
    }
}
