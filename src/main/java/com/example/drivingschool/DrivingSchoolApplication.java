package com.example.drivingschool;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// 驾校培训与考试管理系统启动类，@MapperScan 让 MyBatis 自动扫描 mapper 包下的映射器接口
@SpringBootApplication
@MapperScan("com.example.drivingschool.mapper")
public class DrivingSchoolApplication {

    public static void main(String[] args) {
        SpringApplication.run(DrivingSchoolApplication.class, args);
    }
}
