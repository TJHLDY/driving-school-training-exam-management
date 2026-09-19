package com.example.drivingschool.controller;

import com.example.drivingschool.common.ApiResponse;
import com.example.drivingschool.common.PageResult;
import com.example.drivingschool.dto.EnrollmentDtos;
import com.example.drivingschool.service.EnrollmentService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/enrollments")
public class EnrollmentController {
    private final EnrollmentService enrollmentService;

    public EnrollmentController(EnrollmentService enrollmentService) {
        this.enrollmentService = enrollmentService;
    }

    @PostMapping
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<EnrollmentDtos.EnrollmentView> submit(
            @Valid @RequestBody EnrollmentDtos.SubmitRequest request) {
        return ApiResponse.ok(enrollmentService.submit(request));
    }

    @GetMapping
    public ApiResponse<PageResult<EnrollmentDtos.EnrollmentView>> list(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return ApiResponse.ok(enrollmentService.list(status, page, pageSize));
    }

    @GetMapping("/{enrollmentId}")
    public ApiResponse<EnrollmentDtos.EnrollmentView> get(@PathVariable Long enrollmentId) {
        return ApiResponse.ok(enrollmentService.get(enrollmentId));
    }

    @PostMapping("/{enrollmentId}/review")
    @PreAuthorize("hasRole('ACADEMIC')")
    public ApiResponse<EnrollmentDtos.EnrollmentView> review(
            @PathVariable Long enrollmentId, @Valid @RequestBody EnrollmentDtos.ReviewRequest request) {
        return ApiResponse.ok(enrollmentService.review(enrollmentId, request));
    }

    @PostMapping("/{enrollmentId}/payment")
    @PreAuthorize("hasRole('FINANCE')")
    public ApiResponse<EnrollmentDtos.EnrollmentView> pay(
            @PathVariable Long enrollmentId, @Valid @RequestBody EnrollmentDtos.PaymentRequest request) {
        return ApiResponse.ok(enrollmentService.pay(enrollmentId, request));
    }
}
