package com.example.drivingschool.config;
import java.io.Serializable;
import java.util.List;

// 登录成功后放进 Session 的用户信息
public class SessionUser implements Serializable {
    private final Long userId;
    private final String username;
    private final String realName;
    private final List<String> roleCodes;
    private final String roleNames;
    public SessionUser(Long userId, String username, String realName,
                       List<String> roleCodes, String roleNames) {
        this.userId = userId;
        this.username = username;
        this.realName = realName;
        this.roleCodes = roleCodes;
        this.roleNames = roleNames;
    }
    public boolean hasRole(String roleCode) {
        return roleCodes != null && roleCodes.contains(roleCode);
    }
    public boolean hasAnyRole(String... codes) {
        for (String code : codes) {
            if (hasRole(code)) {
                return true;
            }
        }
        return false;
    }
    // 只拥有某一个角色时才算纯角色，用于决定页面显示本人数据还是全部数据
    public boolean isOnlyRole(String roleCode) {
        return roleCodes != null && roleCodes.size() == 1 && roleCodes.contains(roleCode);
    }
    public Long getUserId() {
        return userId;
    }
    public String getUsername() {
        return username;
    }
    public String getRealName() {
        return realName;
    }
    public List<String> getRoleCodes() {
        return roleCodes;
    }
    public String getRoleNames() {
        return roleNames;
    }
}
