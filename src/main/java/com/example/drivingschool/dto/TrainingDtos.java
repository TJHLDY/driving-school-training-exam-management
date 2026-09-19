package com.example.drivingschool.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public final class TrainingDtos {

    private TrainingDtos() {
    }

    public record VehicleRequest(
            @NotBlank @Size(max = 20) String plateNo,
            @NotBlank @Pattern(regexp = "^(C1|C2)$", message = "只能选择C1或C2") String licenseType,
            @NotNull Boolean enabled
    ) {
    }

    public record VehicleView(String vehicleId, String plateNo, String licenseType, Boolean enabled) {
    }

    public record OpenBookingRequest(
            @NotNull LocalDateTime plannedStart,
            @NotNull LocalDateTime plannedEnd,
            @NotBlank @Size(max = 80) String location
    ) {
    }

    public record AssignRequest(@NotNull Long coachUserId, @NotNull Long vehicleId) {
    }

    public record CompleteRequest(
            @NotNull LocalDateTime actualStart,
            @NotNull LocalDateTime actualEnd,
            @NotNull @DecimalMin("0.00") BigDecimal validHours,
            @Size(max = 200) String resultNote
    ) {
    }

    public record BookingView(String bookingId, String studentUserId, String coachUserId, String vehicleId,
                              LocalDateTime plannedStart, LocalDateTime plannedEnd, String location,
                              String status, String createdBy, LocalDateTime requestedAt,
                              String assignedBy, LocalDateTime assignedAt, LocalDateTime actualStart,
                              LocalDateTime actualEnd, BigDecimal validHours, String resultNote,
                              LocalDateTime cancelledAt) {
    }

    public record TrainingHoursView(String studentUserId, BigDecimal completedHours) {
    }
}
