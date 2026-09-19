package com.example.drivingschool.dto;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
// 报名详情：报名记录连同审核人、收款人的姓名一起返回
@Data

public class EnrollmentDetailDto {
    private Long enrollmentId;
    private Long studentUserId;
    private String studentName;
    private String phone;
    private String licenseType;
    private BigDecimal requiredAmount;
    private BigDecimal plannedHours;
    private String status;
    private LocalDateTime submittedAt;
    private Long reviewedBy;
    private String reviewedByName;
    private LocalDateTime reviewedAt;
    private String reviewNote;
    private BigDecimal paidAmount;
    private String paymentMethod;
    private String paymentVoucher;
    private Long paidBy;
    private String paidByName;
    private LocalDateTime paidAt;
}
