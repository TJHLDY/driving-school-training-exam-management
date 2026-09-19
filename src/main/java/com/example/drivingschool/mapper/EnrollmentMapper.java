package com.example.drivingschool.mapper;
import com.example.drivingschool.dto.EnrollmentDetailDto;
import com.example.drivingschool.entity.Enrollment;
import org.apache.ibatis.annotations.Param;
import java.math.BigDecimal;
import java.util.List;

// 报名与入学模块：报名映射器，缴费只用一次更新完成
public interface EnrollmentMapper {
    Enrollment selectById(@Param("enrollmentId") Long enrollmentId);
    EnrollmentDetailDto selectDetailById(@Param("enrollmentId") Long enrollmentId);
    List<EnrollmentDetailDto> selectDetailList(@Param("studentUserId") Long studentUserId,
                                               @Param("status") String status,
                                               @Param("keyword") String keyword);
    Enrollment selectByStudentUserId(@Param("studentUserId") Long studentUserId);
    int insert(Enrollment enrollment);
    // 驳回后修改原记录重新提交，不新建重复报名
    int updateAndResubmit(@Param("enrollmentId") Long enrollmentId,
                          @Param("licenseType") String licenseType,
                          @Param("availableAmount") BigDecimal availableAmount);
    // 教务审核：确认费用和学时并写审核结论
    int review(@Param("enrollmentId") Long enrollmentId,
               @Param("status") String status,
               @Param("requiredAmount") BigDecimal requiredAmount,
               @Param("plannedHours") BigDecimal plannedHours,
               @Param("reviewedBy") Long reviewedBy,
               @Param("reviewNote") String reviewNote);
    // 财务登记一次全额缴费：只允许 APPROVED 状态，返回 0 表示已经缴过费，避免重复收款
    int registerPayment(@Param("enrollmentId") Long enrollmentId,
                        @Param("paidAmount") BigDecimal paidAmount,
                        @Param("paymentMethod") String paymentMethod,
                        @Param("paymentVoucher") String paymentVoucher,
                        @Param("paidBy") Long paidBy);
    int markWithdrawn(@Param("enrollmentId") Long enrollmentId);
}
