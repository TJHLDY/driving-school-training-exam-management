package com.example.drivingschool.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public final class EnrollmentDtos {

    private EnrollmentDtos() {
    }

    public record SubmitRequest(
            @NotBlank @Pattern(regexp = "^(C1|C2)$", message = "只能选择C1或C2") String licenseType
    ) {
    }

    public record ReviewRequest(
            @NotNull Boolean approved,
            @DecimalMin(value = "0.01") BigDecimal requiredAmount,
            @DecimalMin(value = "0.01") BigDecimal plannedHours,
            @Size(max = 200) String reviewNote
    ) {
    }

    public record PaymentRequest(
            @NotBlank @Size(max = 20) String paymentMethod,
            @NotBlank @Size(max = 80) String paymentVoucher
    ) {
    }

    public record EnrollmentView(String enrollmentId, String studentUserId, String licenseType,
                                 BigDecimal requiredAmount, BigDecimal plannedHours, String status,
                                 LocalDateTime submittedAt, String reviewedBy, LocalDateTime reviewedAt,
                                 String reviewNote, BigDecimal paidAmount, String paymentMethod,
                                 String paymentVoucher, String paidBy, LocalDateTime paidAt) {
    }
}
