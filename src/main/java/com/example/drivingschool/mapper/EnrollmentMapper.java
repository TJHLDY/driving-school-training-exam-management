package com.example.drivingschool.mapper;

import com.example.drivingschool.entity.Enrollment;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface EnrollmentMapper {
    Enrollment findById(@Param("enrollmentId") Long enrollmentId);
    Enrollment findByIdForUpdate(@Param("enrollmentId") Long enrollmentId);
    Enrollment findByStudentUserId(@Param("studentUserId") Long studentUserId);
    Enrollment findByStudentUserIdForUpdate(@Param("studentUserId") Long studentUserId);
    int insert(Enrollment enrollment);
    int updateStudentSubmission(Enrollment enrollment);
    int review(@Param("enrollmentId") Long enrollmentId, @Param("status") String status,
               @Param("requiredAmount") BigDecimal requiredAmount, @Param("plannedHours") BigDecimal plannedHours,
               @Param("reviewedBy") Long reviewedBy, @Param("reviewedAt") LocalDateTime reviewedAt,
               @Param("reviewNote") String reviewNote);
    int pay(@Param("enrollmentId") Long enrollmentId, @Param("paidAmount") BigDecimal paidAmount,
            @Param("paymentMethod") String paymentMethod, @Param("paymentVoucher") String paymentVoucher,
            @Param("paidBy") Long paidBy, @Param("paidAt") LocalDateTime paidAt);
    int markWithdrawn(@Param("enrollmentId") Long enrollmentId);
    List<Enrollment> findPage(@Param("studentUserId") Long studentUserId, @Param("status") String status,
                              @Param("offset") long offset, @Param("size") int size);
    long count(@Param("studentUserId") Long studentUserId, @Param("status") String status);
}
