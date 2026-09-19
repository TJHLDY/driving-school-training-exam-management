package com.example.drivingschool.controller;
import com.example.drivingschool.config.SessionUser;
import com.example.drivingschool.entity.Menu;
import com.example.drivingschool.entity.Role;
import com.example.drivingschool.entity.UserAccount;
import com.example.drivingschool.mapper.MenuMapper;
import com.example.drivingschool.mapper.RoleMapper;
import com.example.drivingschool.mapper.UserAccountMapper;
import com.example.drivingschool.mapper.UserRoleMapper;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
@Controller

public class AdminController {
    @Autowired private UserAccountMapper userAccountMapper;
    @Autowired private RoleMapper roleMapper;
    @Autowired private UserRoleMapper userRoleMapper;
    @Autowired private MenuMapper menuMapper;
    private boolean notAdmin(HttpSession session) {
        SessionUser user = (SessionUser) session.getAttribute("loginUser");
        return !user.hasRole("ADMIN");
    }
    @GetMapping("/users")
    public String users(HttpSession session, Model model) {
        if (notAdmin(session)) {
            return "redirect:/";
        }
        List<UserAccount> users = userAccountMapper.selectList(null, null);
        Map<Long, String> userRoles = users.stream().collect(Collectors.toMap(
                UserAccount::getUserId,
                u -> roleMapper.selectByUserId(u.getUserId()).stream()
                        .map(Role::getRoleName).collect(Collectors.joining(" / "))));
        model.addAttribute("users", users);
        model.addAttribute("userRoles", userRoles);
        model.addAttribute("roles", roleMapper.selectList());
        return "admin/users";
    }
    @Transactional
    @PostMapping("/users/{id}/roles")
    public String assignRoles(@PathVariable Long id, @RequestParam(required = false) List<Long> roleIds,
                              HttpSession session, RedirectAttributes ra) {
        if (notAdmin(session)) {
            return "redirect:/";
        }
        userRoleMapper.deleteByUserId(id);
        if (roleIds != null && !roleIds.isEmpty()) {
            userRoleMapper.insertBatch(id, roleIds);
        }
        ra.addFlashAttribute("msg", "角色已重新分配");
        return "redirect:/users";
    }
    @PostMapping("/users/{id}/status")
    public String toggleStatus(@PathVariable Long id, @RequestParam Boolean enabled,
                               HttpSession session, RedirectAttributes ra) {
        if (notAdmin(session)) {
            return "redirect:/";
        }
        userAccountMapper.updateEnabled(id, enabled);
        ra.addFlashAttribute("msg", Boolean.TRUE.equals(enabled) ? "账号已启用" : "账号已停用");
        return "redirect:/users";
    }
    @GetMapping("/roles")
    public String roles(HttpSession session, Model model) {
        if (notAdmin(session)) {
            return "redirect:/";
        }
        // 菜单数量先在后台算好，模板里直接取，避免在 Thymeleaf 表达式里做集合筛选
        Map<Long, Long> menuCount = menuMapper.selectList(null, null).stream()
                .collect(Collectors.groupingBy(Menu::getRoleId, Collectors.counting()));
        model.addAttribute("roles", roleMapper.selectList());
        model.addAttribute("menuCount", menuCount);
        return "admin/roles";
    }
    @PostMapping("/roles")
    public String addRole(@RequestParam String roleCode, @RequestParam String roleName,
                          HttpSession session, RedirectAttributes ra) {
        if (notAdmin(session)) {
            return "redirect:/";
        }
        if (roleMapper.countByCode(roleCode) > 0) {
            ra.addFlashAttribute("err", "角色编码已存在");
            return "redirect:/roles";
        }
        Role role = new Role();
        role.setRoleCode(roleCode.toUpperCase());
        role.setRoleName(roleName);
        roleMapper.insert(role);
        ra.addFlashAttribute("msg", "角色已新增");
        return "redirect:/roles";
    }
    @GetMapping("/menus")
    public String menus(HttpSession session, Model model) {
        if (notAdmin(session)) {
            return "redirect:/";
        }
        model.addAttribute("menus", menuMapper.selectList(null, null));
        model.addAttribute("roles", roleMapper.selectList());
        return "admin/menus";
    }
    @PostMapping("/menus")
    public String addMenu(@RequestParam Long roleId, @RequestParam String menuCode,
                          @RequestParam String menuName, @RequestParam String routePath,
                          @RequestParam(defaultValue = "0") Integer sortNo,
                          HttpSession session, RedirectAttributes ra) {
        if (notAdmin(session)) {
            return "redirect:/";
        }
        if (menuMapper.countByRoleIdAndCode(roleId, menuCode) > 0) {
            ra.addFlashAttribute("err", "该角色下菜单编码已存在");
            return "redirect:/menus";
        }
        Menu menu = new Menu();
        menu.setRoleId(roleId);
        menu.setMenuCode(menuCode);
        menu.setMenuName(menuName);
        menu.setRoutePath(routePath);
        menu.setSortNo(sortNo);
        menu.setEnabled(true);
        menuMapper.insert(menu);
        ra.addFlashAttribute("msg", "菜单已新增");
        return "redirect:/menus";
    }
}
