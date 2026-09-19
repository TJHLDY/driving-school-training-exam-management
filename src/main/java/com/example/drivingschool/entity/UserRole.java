package com.example.drivingschool.entity;
import lombok.Data;
// 对应 user_role 表：用户拥有的角色，联合主键 (user_id, role_id)
@Data

public class UserRole {
    private Long userId;
    private Long roleId;
}
