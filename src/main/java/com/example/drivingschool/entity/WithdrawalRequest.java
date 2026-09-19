package com.example.drivingschool.entity;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
// 对应 withdrawal_request 表：退学申请、核定及一次退款
@Data

public class WithdrawalRequest {
    private Long withdrawalId;
    private Long enrollmentId;
    private String reason;
    private LocalDateTime requestedAt;
    private String status;
    private Long reviewedBy;
    private LocalDateTime reviewedAt;
    private String reviewNote;
    private BigDecimal approvedRefundAmount;
    private BigDecimal refundAmount;
    private String refundMethod;
    private String refundVoucher;
    private Long refundedBy;
    private LocalDateTime refundedAt;
}
