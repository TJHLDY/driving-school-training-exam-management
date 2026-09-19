package com.example.drivingschool.entity;
import lombok.Data;
// 对应 role 表：角色
@Data

public class Role {
    private Long roleId;
    private String roleCode;
    private String roleName;
}
