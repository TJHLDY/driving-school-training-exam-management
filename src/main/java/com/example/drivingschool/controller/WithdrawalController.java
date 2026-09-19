package com.example.drivingschool.controller;

import com.example.drivingschool.common.ApiResponse;
import com.example.drivingschool.common.PageResult;
import com.example.drivingschool.dto.WithdrawalDtos;
import com.example.drivingschool.service.WithdrawalService;
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
@RequestMapping("/api/withdrawals")
public class WithdrawalController {
    private final WithdrawalService withdrawalService;

    public WithdrawalController(WithdrawalService withdrawalService) {
        this.withdrawalService = withdrawalService;
    }

    @PostMapping
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<WithdrawalDtos.WithdrawalView> submit(
            @Valid @RequestBody WithdrawalDtos.SubmitRequest request) {
        return ApiResponse.ok(withdrawalService.submit(request));
    }

    @GetMapping
    public ApiResponse<PageResult<WithdrawalDtos.WithdrawalView>> list(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return ApiResponse.ok(withdrawalService.list(status, page, pageSize));
    }

    @GetMapping("/{withdrawalId}")
    public ApiResponse<WithdrawalDtos.WithdrawalView> get(@PathVariable Long withdrawalId) {
        return ApiResponse.ok(withdrawalService.get(withdrawalId));
    }

    @PostMapping("/{withdrawalId}/review")
    @PreAuthorize("hasRole('ACADEMIC')")
    public ApiResponse<WithdrawalDtos.WithdrawalView> review(
            @PathVariable Long withdrawalId, @Valid @RequestBody WithdrawalDtos.ReviewRequest request) {
        return ApiResponse.ok(withdrawalService.review(withdrawalId, request));
    }

    @PostMapping("/{withdrawalId}/refund")
    @PreAuthorize("hasRole('FINANCE')")
    public ApiResponse<WithdrawalDtos.WithdrawalView> refund(
            @PathVariable Long withdrawalId, @Valid @RequestBody WithdrawalDtos.RefundRequest request) {
        return ApiResponse.ok(withdrawalService.refund(withdrawalId, request));
    }
}
