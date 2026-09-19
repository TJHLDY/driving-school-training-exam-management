package com.example.drivingschool.mapper;
import com.example.drivingschool.entity.Role;
import org.apache.ibatis.annotations.Param;
import java.util.List;

// 用户管理模块：角色映射器
public interface RoleMapper {
    Role selectById(@Param("roleId") Long roleId);
    Role selectByCode(@Param("roleCode") String roleCode);
    List<Role> selectList();
    // 查某个用户拥有的全部角色，多角色用户会返回多行
    List<Role> selectByUserId(@Param("userId") Long userId);
    int countByCode(@Param("roleCode") String roleCode);
    int insert(Role role);
    int updateById(Role role);
    int countUserReference(@Param("roleId") Long roleId);
    int deleteById(@Param("roleId") Long roleId);
}
