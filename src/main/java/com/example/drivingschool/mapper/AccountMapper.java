package com.example.drivingschool.mapper;

import com.example.drivingschool.entity.Menu;
import com.example.drivingschool.entity.Role;
import com.example.drivingschool.entity.UserAccount;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface AccountMapper {
    UserAccount findById(@Param("userId") Long userId);
    UserAccount findByIdForUpdate(@Param("userId") Long userId);
    UserAccount findByUsername(@Param("username") String username);
    List<String> findRoleCodesByUserId(@Param("userId") Long userId);
    long countUsersByRoleCode(@Param("roleCode") String roleCode);
    List<Role> findRoles();
    Long findRoleIdByCode(@Param("roleCode") String roleCode);
    List<Menu> findMenusByUserId(@Param("userId") Long userId);
    List<Menu> findMenus();
    Menu findMenuById(@Param("menuId") Long menuId);
    List<UserAccount> findUsers(@Param("keyword") String keyword, @Param("offset") long offset, @Param("size") int size);
    long countUsers(@Param("keyword") String keyword);
    int insertUser(UserAccount user);
    int updateUser(UserAccount user);
    int updatePassword(@Param("userId") Long userId, @Param("passwordHash") String passwordHash);
    int updateUserEnabled(@Param("userId") Long userId, @Param("enabled") boolean enabled);
    int deleteUserRoles(@Param("userId") Long userId);
    int insertUserRole(@Param("userId") Long userId, @Param("roleId") Long roleId);
    int insertRole(Role role);
    int updateRole(Role role);
    int insertMenu(Menu menu);
    int updateMenu(Menu menu);
}
