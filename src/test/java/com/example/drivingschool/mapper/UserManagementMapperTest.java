package com.example.drivingschool.mapper;

import com.example.drivingschool.entity.Menu;
import com.example.drivingschool.entity.Role;
import com.example.drivingschool.entity.UserAccount;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** 用户管理模块映射器验证，断言数据来自 database/02_base_seed.sql */
@SpringBootTest
class UserManagementMapperTest {

    @Autowired private UserAccountMapper userAccountMapper;
    @Autowired private RoleMapper roleMapper;
    @Autowired private UserRoleMapper userRoleMapper;
    @Autowired private MenuMapper menuMapper;

    @Test
    @DisplayName("按用户名查 admin，验证字段映射与布尔字段")
    void selectAdmin() {
        UserAccount admin = userAccountMapper.selectByUsername("admin");

        assertThat(admin.getUserId()).isEqualTo(1L);
        assertThat(admin.getRealName()).isEqualTo("管理员");
        assertThat(admin.getPhone()).isEqualTo("13800000001");
        assertThat(admin.getEnabled()).isTrue();
        assertThat(admin.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("academic 同时拥有教务和教练两个角色")
    void multiRoles() {
        List<Role> roles = roleMapper.selectByUserId(2L);

        assertThat(roles).extracting(Role::getRoleCode)
                .containsExactlyInAnyOrder("ACADEMIC", "COACH");
    }

    @Test
    @DisplayName("菜单按角色查并按 route_path 去重：academic 得到 4 条不重复菜单")
    void menusDeduplicatedByRoutePath() {
        List<Menu> menus = menuMapper.selectByUserId(2L);

        assertThat(menus).extracting(Menu::getRoutePath)
                .containsExactly("/enrollments", "/training", "/exams", "/withdrawals");
        assertThat(menus).extracting(Menu::getSortNo).isSorted();

        // 学员只有一个角色，菜单就是四条
        assertThat(menuMapper.selectByUserId(5L)).hasSize(4);
        // 财务只有两条菜单
        assertThat(menuMapper.selectByUserId(3L)).extracting(Menu::getRoutePath)
                .containsExactly("/enrollments", "/withdrawals");
        // 教练看不到报名和退费
        assertThat(menuMapper.selectByUserId(4L)).extracting(Menu::getRoutePath)
                .containsExactly("/training", "/exams");
    }

    @Test
    @Transactional
    @DisplayName("新增账号回填自增主键，重复分配角色被主键约束挡住")
    void insertUserAndAssignRole() {
        UserAccount user = new UserAccount();
        user.setUsername("test_user");
        user.setPasswordHash("$2b$12$test");
        user.setRealName("测试账号");
        user.setPhone("13900000099");

        assertThat(userAccountMapper.insert(user)).isEqualTo(1);
        assertThat(user.getUserId()).isNotNull();

        userRoleMapper.insert(user.getUserId(), 5L);
        assertThat(userRoleMapper.countByUserIdAndRoleId(user.getUserId(), 5L)).isEqualTo(1);

        // 有业务引用前可以删；删账号前必须先清掉角色关联，否则外键不允许
        assertThat(userAccountMapper.countBusinessReference(user.getUserId())).isZero();
        userRoleMapper.deleteByUserId(user.getUserId());
        assertThat(userAccountMapper.deleteById(user.getUserId())).isEqualTo(1);
    }
}
