package com.example.drivingschool.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class TrainingBooking {
    private Long bookingId;
    private Long studentUserId;
    private Long coachUserId;
    private Long vehicleId;
    private LocalDateTime plannedStart;
    private LocalDateTime plannedEnd;
    private String location;
    private String status;
    private Long createdBy;
    private LocalDateTime requestedAt;
    private Long assignedBy;
    private LocalDateTime assignedAt;
    private LocalDateTime actualStart;
    private LocalDateTime actualEnd;
    private BigDecimal validHours;
    private String resultNote;
    private LocalDateTime cancelledAt;
}
