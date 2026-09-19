package com.example.drivingschool.controller;

import com.example.drivingschool.common.ApiResponse;
import com.example.drivingschool.common.PageResult;
import com.example.drivingschool.dto.TrainingDtos;
import com.example.drivingschool.service.TrainingService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/training")
public class TrainingController {
    private final TrainingService trainingService;

    public TrainingController(TrainingService trainingService) {
        this.trainingService = trainingService;
    }

    @GetMapping("/vehicles")
    public ApiResponse<List<TrainingDtos.VehicleView>> vehicles(
            @RequestParam(required = false) Boolean enabled) {
        return ApiResponse.ok(trainingService.listVehicles(enabled));
    }

    @PostMapping("/vehicles")
    @PreAuthorize("hasRole('ACADEMIC')")
    public ApiResponse<TrainingDtos.VehicleView> createVehicle(
            @Valid @RequestBody TrainingDtos.VehicleRequest request) {
        return ApiResponse.ok(trainingService.createVehicle(request));
    }

    @PutMapping("/vehicles/{vehicleId}")
    @PreAuthorize("hasRole('ACADEMIC')")
    public ApiResponse<TrainingDtos.VehicleView> updateVehicle(
            @PathVariable Long vehicleId, @Valid @RequestBody TrainingDtos.VehicleRequest request) {
        return ApiResponse.ok(trainingService.updateVehicle(vehicleId, request));
    }

    @GetMapping("/bookings")
    public ApiResponse<PageResult<TrainingDtos.BookingView>> bookings(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return ApiResponse.ok(trainingService.list(status, page, pageSize));
    }

    @GetMapping("/bookings/{bookingId}")
    public ApiResponse<TrainingDtos.BookingView> booking(@PathVariable Long bookingId) {
        return ApiResponse.ok(trainingService.get(bookingId));
    }

    @PostMapping("/bookings/open")
    @PreAuthorize("hasRole('ACADEMIC')")
    public ApiResponse<TrainingDtos.BookingView> open(
            @Valid @RequestBody TrainingDtos.OpenBookingRequest request) {
        return ApiResponse.ok(trainingService.createOpenBooking(request));
    }

    @PostMapping("/bookings/{bookingId}/claim")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<TrainingDtos.BookingView> claim(@PathVariable Long bookingId) {
        return ApiResponse.ok(trainingService.claim(bookingId));
    }

    @PostMapping("/bookings/{bookingId}/assign")
    @PreAuthorize("hasRole('ACADEMIC')")
    public ApiResponse<TrainingDtos.BookingView> assign(
            @PathVariable Long bookingId, @Valid @RequestBody TrainingDtos.AssignRequest request) {
        return ApiResponse.ok(trainingService.assign(bookingId, request));
    }

    @PostMapping("/bookings/{bookingId}/cancel")
    public ApiResponse<TrainingDtos.BookingView> cancel(@PathVariable Long bookingId) {
        return ApiResponse.ok(trainingService.cancel(bookingId));
    }

    @PostMapping("/bookings/{bookingId}/complete")
    @PreAuthorize("hasRole('COACH')")
    public ApiResponse<TrainingDtos.BookingView> complete(
            @PathVariable Long bookingId, @Valid @RequestBody TrainingDtos.CompleteRequest request) {
        return ApiResponse.ok(trainingService.complete(bookingId, request));
    }

    @GetMapping("/hours/{studentUserId}")
    public ApiResponse<TrainingDtos.TrainingHoursView> hours(@PathVariable Long studentUserId) {
        return ApiResponse.ok(trainingService.completedHours(studentUserId));
    }
}
