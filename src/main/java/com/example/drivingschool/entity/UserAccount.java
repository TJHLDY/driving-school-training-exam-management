package com.example.drivingschool.entity;

import lombok.Data;

import java.time.LocalDateTime;

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
