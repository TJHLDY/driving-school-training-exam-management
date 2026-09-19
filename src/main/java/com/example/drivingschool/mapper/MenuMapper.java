package com.example.drivingschool.mapper;
import com.example.drivingschool.entity.Menu;
import org.apache.ibatis.annotations.Param;
import java.util.List;

// 用户管理模块：菜单映射器
public interface MenuMapper {
    Menu selectById(@Param("menuId") Long menuId);
    List<Menu> selectList(@Param("roleId") Long roleId, @Param("enabled") Boolean enabled);
    // 查某个用户能看到的菜单，按 route_path 去重，多角色共用同一路径时只留一条
    List<Menu> selectByUserId(@Param("userId") Long userId);
    int countByRoleIdAndCode(@Param("roleId") Long roleId, @Param("menuCode") String menuCode);
    int insert(Menu menu);
    int updateById(Menu menu);
    int updateEnabled(@Param("menuId") Long menuId, @Param("enabled") Boolean enabled);
    int deleteById(@Param("menuId") Long menuId);
}
