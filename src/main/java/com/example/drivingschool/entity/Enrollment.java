package com.example.drivingschool.entity;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
// 对应 enrollment 表：报名、审核及一次缴费，一位学员只有一份报名记录
@Data

public class Enrollment {
    private Long enrollmentId;
    private Long studentUserId;
    private String licenseType;
    private BigDecimal requiredAmount;
    private BigDecimal plannedHours;
    private String status;
    private LocalDateTime submittedAt;
    private Long reviewedBy;
    private LocalDateTime reviewedAt;
    private String reviewNote;
    private BigDecimal paidAmount;
    private String paymentMethod;
    private String paymentVoucher;
    private Long paidBy;
    private LocalDateTime paidAt;
}
