package com.example.drivingschool.entity;

import lombok.Data;

@Data
public class Menu {
    private Long menuId;
    private Long roleId;
    private String menuCode;
    private String menuName;
    private String routePath;
    private Integer sortNo;
    private Boolean enabled;
}
