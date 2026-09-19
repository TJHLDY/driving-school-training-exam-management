package com.example.drivingschool.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public final class WithdrawalDtos {

    private WithdrawalDtos() {
    }

    public record SubmitRequest(@NotBlank @Size(max = 200) String reason) {
    }

    public record ReviewRequest(
            @NotNull Boolean approved,
            @DecimalMin("0.00") BigDecimal approvedRefundAmount,
            @Size(max = 200) String reviewNote
    ) {
    }

    public record RefundRequest(
            @NotBlank @Size(max = 20) String refundMethod,
            @NotBlank @Size(max = 80) String refundVoucher
    ) {
    }

    public record WithdrawalView(String withdrawalId, String enrollmentId, String studentUserId, String reason,
                                 LocalDateTime requestedAt, String status, String reviewedBy,
                                 LocalDateTime reviewedAt, String reviewNote, BigDecimal approvedRefundAmount,
                                 BigDecimal refundAmount, String refundMethod, String refundVoucher,
                                 String refundedBy, LocalDateTime refundedAt) {
    }
}
