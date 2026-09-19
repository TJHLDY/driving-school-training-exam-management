package com.example.drivingschool;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
@MapperScan("com.example.drivingschool.mapper")
public class DrivingSchoolApplication {

    public static void main(String[] args) {
        SpringApplication.run(DrivingSchoolApplication.class, args);
    }
}
