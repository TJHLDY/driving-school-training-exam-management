package com.example.drivingschool.mapper;

import com.example.drivingschool.entity.WithdrawalRequest;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface WithdrawalMapper {
    WithdrawalRequest findById(@Param("withdrawalId") Long withdrawalId);
    WithdrawalRequest findByIdForUpdate(@Param("withdrawalId") Long withdrawalId);
    WithdrawalRequest findByEnrollmentId(@Param("enrollmentId") Long enrollmentId);
    WithdrawalRequest findByEnrollmentIdForUpdate(@Param("enrollmentId") Long enrollmentId);
    int insert(WithdrawalRequest withdrawal);
    int resubmit(@Param("withdrawalId") Long withdrawalId, @Param("reason") String reason,
                 @Param("requestedAt") LocalDateTime requestedAt);
    int review(@Param("withdrawalId") Long withdrawalId, @Param("status") String status,
               @Param("reviewedBy") Long reviewedBy, @Param("reviewedAt") LocalDateTime reviewedAt,
               @Param("reviewNote") String reviewNote, @Param("approvedRefundAmount") BigDecimal approvedRefundAmount);
    int refund(@Param("withdrawalId") Long withdrawalId, @Param("refundAmount") BigDecimal refundAmount,
               @Param("refundMethod") String refundMethod, @Param("refundVoucher") String refundVoucher,
               @Param("refundedBy") Long refundedBy, @Param("refundedAt") LocalDateTime refundedAt);
    List<WithdrawalRequest> findPage(@Param("studentUserId") Long studentUserId, @Param("status") String status,
                                     @Param("offset") long offset, @Param("size") int size);
    long count(@Param("studentUserId") Long studentUserId, @Param("status") String status);
}
