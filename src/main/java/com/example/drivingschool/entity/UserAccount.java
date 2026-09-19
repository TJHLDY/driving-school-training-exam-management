package com.example.drivingschool.entity;
import lombok.Data;
import java.time.LocalDateTime;
// 对应 user_account 表：账号与个人资料，学员和教练都在这张表，用 id_number / coach_license 区分身份
@Data

public class UserAccount {
    private Long userId;
    private String username;
    private String passwordHash;
    private String realName;
    private String phone;
    private String gender;
    private String idNumber;
    private String coachLicense;
    private Boolean enabled;
    private LocalDateTime createdAt;
}
