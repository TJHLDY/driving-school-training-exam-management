package com.example.drivingschool.controller;
import com.example.drivingschool.config.SessionUser;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
@Controller

public class HomeController {
    @GetMapping("/")
    public String home(HttpSession session, Model model) {
        SessionUser user = (SessionUser) session.getAttribute("loginUser");
        model.addAttribute("isStudent", user.hasRole("STUDENT"));
        model.addAttribute("isAcademic", user.hasRole("ACADEMIC"));
        model.addAttribute("isCoach", user.hasRole("COACH"));
        model.addAttribute("isFinance", user.hasRole("FINANCE"));
        model.addAttribute("isAdmin", user.hasRole("ADMIN"));
        return "home";
    }
    @GetMapping("/profile")
    public String profile() {
        return "profile";
    }
}
