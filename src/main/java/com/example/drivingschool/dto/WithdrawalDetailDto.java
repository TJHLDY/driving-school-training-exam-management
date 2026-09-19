package com.example.drivingschool.dto;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
// 退学退费详情：申请连同学员、报名实缴金额和退款信息
@Data

public class WithdrawalDetailDto {
    private Long withdrawalId;
    private Long enrollmentId;
    private Long studentUserId;
    private String studentName;
    private String phone;
    private String licenseType;
    private String enrollmentStatus;
    private String reason;
    private LocalDateTime requestedAt;
    private String status;
    private Long reviewedBy;
    private String reviewedByName;
    private LocalDateTime reviewedAt;
    private String reviewNote;
    private BigDecimal approvedRefundAmount;
    private BigDecimal paidAmount;
    private BigDecimal refundAmount;
    private String refundMethod;
    private String refundVoucher;
    private LocalDateTime refundedAt;
}
