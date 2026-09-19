package com.example.drivingschool.entity;
import lombok.Data;
// 对应 menu 表：一行表示某个角色的一项菜单，不再有独立的角色菜单关系表
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
