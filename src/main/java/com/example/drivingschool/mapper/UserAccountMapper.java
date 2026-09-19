package com.example.drivingschool.mapper;
import com.example.drivingschool.entity.UserAccount;
import org.apache.ibatis.annotations.Param;
import java.util.List;

// 用户管理模块：账号与个人资料映射器
public interface UserAccountMapper {
    UserAccount selectById(@Param("userId") Long userId);
    UserAccount selectByUsername(@Param("username") String username);
    List<UserAccount> selectList(@Param("keyword") String keyword, @Param("enabled") Boolean enabled);
    int countByUsername(@Param("username") String username);
    int countByPhone(@Param("phone") String phone);
    int countByIdNumber(@Param("idNumber") String idNumber);
    int insert(UserAccount userAccount);
    int updateById(UserAccount userAccount);
    int updatePassword(@Param("userId") Long userId, @Param("passwordHash") String passwordHash);
    int updateEnabled(@Param("userId") Long userId, @Param("enabled") Boolean enabled);
    // 删除账号前检查业务引用：有报名、培训、答卷、退费记录就不允许删
    int countBusinessReference(@Param("userId") Long userId);
    int deleteById(@Param("userId") Long userId);
}
