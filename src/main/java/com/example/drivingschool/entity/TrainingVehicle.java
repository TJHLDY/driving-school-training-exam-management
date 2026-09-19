package com.example.drivingschool.entity;
import lombok.Data;
// 对应 training_vehicle 表：培训车辆，车型必须与学员报名类型一致才能分配
@Data

public class TrainingVehicle {
    private Long vehicleId;
    private String plateNo;
    private String licenseType;
    private Boolean enabled;
}
