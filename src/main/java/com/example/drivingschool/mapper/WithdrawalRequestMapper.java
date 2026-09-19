package com.example.drivingschool.mapper;
import com.example.drivingschool.dto.WithdrawalDetailDto;
import com.example.drivingschool.entity.WithdrawalRequest;
import org.apache.ibatis.annotations.Param;
import java.math.BigDecimal;
import java.util.List;

// 退学与退费模块：退学申请和一次退款映射器
public interface WithdrawalRequestMapper {
    WithdrawalRequest selectById(@Param("withdrawalId") Long withdrawalId);
    WithdrawalDetailDto selectDetailById(@Param("withdrawalId") Long withdrawalId);
    List<WithdrawalDetailDto> selectDetailList(@Param("studentUserId") Long studentUserId,
                                               @Param("status") String status);
    // 一份报名只保留一份退学申请
    WithdrawalRequest selectByEnrollmentId(@Param("enrollmentId") Long enrollmentId);
    // 处理中 = SUBMITTED 或 APPROVED，处理中不允许新增预约
    int countProcessingByStudentUserId(@Param("studentUserId") Long studentUserId);
    int insert(WithdrawalRequest withdrawalRequest);
    // 驳回后修改原记录重新提交
    int updateAndResubmit(@Param("withdrawalId") Long withdrawalId, @Param("reason") String reason);
    // 教务核定：通过时写核定金额，驳回时只写意见
    int review(@Param("withdrawalId") Long withdrawalId,
               @Param("status") String status,
               @Param("reviewedBy") Long reviewedBy,
               @Param("reviewNote") String reviewNote,
               @Param("approvedRefundAmount") BigDecimal approvedRefundAmount);
    // 财务登记退款并结算：只允许 APPROVED 且退款额等于核定额，同时把报名改为 WITHDRAWN，重复调用返回 0
    int registerRefund(@Param("withdrawalId") Long withdrawalId,
                       @Param("refundAmount") BigDecimal refundAmount,
                       @Param("refundMethod") String refundMethod,
                       @Param("refundVoucher") String refundVoucher,
                       @Param("refundedBy") Long refundedBy);
}
