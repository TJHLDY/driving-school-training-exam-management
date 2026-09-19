package com.example.drivingschool.mapper;
import com.example.drivingschool.entity.UserRole;
import org.apache.ibatis.annotations.Param;
import java.util.List;

// 用户管理模块：用户角色关系映射器，联合主键 (user_id, role_id)
public interface UserRoleMapper {
    int insert(@Param("userId") Long userId, @Param("roleId") Long roleId);
    int insertBatch(@Param("userId") Long userId, @Param("roleIds") List<Long> roleIds);
    List<UserRole> selectByUserId(@Param("userId") Long userId);
    int countByUserIdAndRoleId(@Param("userId") Long userId, @Param("roleId") Long roleId);
    int delete(@Param("userId") Long userId, @Param("roleId") Long roleId);
    // 重新分配角色前先清空
    int deleteByUserId(@Param("userId") Long userId);
}
