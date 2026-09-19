package com.example.drivingschool.entity;

import lombok.Data;

@Data
public class TrainingVehicle {
    private Long vehicleId;
    private String plateNo;
    private String licenseType;
    private Boolean enabled;
}
