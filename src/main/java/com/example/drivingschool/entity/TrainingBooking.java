package com.example.drivingschool.entity;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
// 对应 training_booking 表：一张表承载培训时段、预约、分配和训练结果，用 status 区分阶段
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
