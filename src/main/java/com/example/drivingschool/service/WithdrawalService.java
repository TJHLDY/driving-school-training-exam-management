package com.example.drivingschool.service;

import com.example.drivingschool.common.PageResult;
import com.example.drivingschool.common.PageUtils;
import com.example.drivingschool.dto.WithdrawalDtos;
import com.example.drivingschool.entity.Enrollment;
import com.example.drivingschool.entity.WithdrawalRequest;
import com.example.drivingschool.exception.BusinessException;
import com.example.drivingschool.mapper.EnrollmentMapper;
import com.example.drivingschool.mapper.TrainingMapper;
import com.example.drivingschool.mapper.WithdrawalMapper;
import com.example.drivingschool.security.LoginUser;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class WithdrawalService {
    private final WithdrawalMapper withdrawalMapper;
    private final EnrollmentMapper enrollmentMapper;
    private final TrainingMapper trainingMapper;
    private final CurrentUserService currentUserService;

    public WithdrawalService(WithdrawalMapper withdrawalMapper, EnrollmentMapper enrollmentMapper,
                             TrainingMapper trainingMapper, CurrentUserService currentUserService) {
        this.withdrawalMapper = withdrawalMapper;
        this.enrollmentMapper = enrollmentMapper;
        this.trainingMapper = trainingMapper;
        this.currentUserService = currentUserService;
    }

    @Transactional
    public WithdrawalDtos.WithdrawalView submit(WithdrawalDtos.SubmitRequest request) {
        LoginUser user = currentUserService.requireUser();
        currentUserService.requireAnyRole("STUDENT");
        Enrollment enrollment = enrollmentMapper.findByStudentUserIdForUpdate(user.userId());
        if (enrollment == null || !"ACTIVE".equals(enrollment.getStatus())) {
            throw new BusinessException(HttpStatus.CONFLICT, "只有生效报名可以申请退学");
        }
        LocalDateTime now = LocalDateTime.now();
        if (trainingMapper.countUnfinishedStarted(user.userId(), now) > 0) {
            throw new BusinessException(HttpStatus.CONFLICT, "存在已开始未结束的培训，请先处理");
        }
        WithdrawalRequest existing = withdrawalMapper.findByEnrollmentIdForUpdate(enrollment.getEnrollmentId());
        if (existing == null) {
            existing = new WithdrawalRequest();
            existing.setEnrollmentId(enrollment.getEnrollmentId());
            existing.setReason(request.reason().trim());
            existing.setRequestedAt(now);
            withdrawalMapper.insert(existing);
            existing = withdrawalMapper.findById(existing.getWithdrawalId());
        } else if ("REJECTED".equals(existing.getStatus())) {
            if (withdrawalMapper.resubmit(existing.getWithdrawalId(), request.reason().trim(), now) != 1) {
                throw new BusinessException(HttpStatus.CONFLICT, "退学申请状态已变化，请刷新后重试");
            }
            existing = withdrawalMapper.findById(existing.getWithdrawalId());
        } else {
            throw new BusinessException(HttpStatus.CONFLICT, "已存在处理中或已完成的退学申请");
        }
        trainingMapper.cancelFutureBookings(user.userId(), now);
        return toView(existing, enrollment.getStudentUserId());
    }

    public WithdrawalDtos.WithdrawalView get(Long withdrawalId) {
        WithdrawalRequest withdrawal = requireWithdrawal(withdrawalId);
        Enrollment enrollment = requireEnrollment(withdrawal.getEnrollmentId());
        requireCanView(enrollment, withdrawal);
        return toView(withdrawal, enrollment.getStudentUserId());
    }

    public PageResult<WithdrawalDtos.WithdrawalView> list(String status, Integer pageNo, Integer pageSize) {
        LoginUser user = currentUserService.requireUser();
        Long studentFilter = user.roleCodes().contains("STUDENT") ? user.userId() : null;
        if (studentFilter == null && user.roleCodes().stream().noneMatch(List.of("ACADEMIC", "FINANCE", "ADMIN")::contains)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "无权查询退学退费记录");
        }
        int page = PageUtils.page(pageNo);
        int size = PageUtils.pageSize(pageSize);
        long offset = PageUtils.offset(page, size);
        List<WithdrawalDtos.WithdrawalView> list = withdrawalMapper.findPage(studentFilter, status, offset, size)
                .stream().map(w -> toView(w, requireEnrollment(w.getEnrollmentId()).getStudentUserId())).toList();
        return new PageResult<>(list, withdrawalMapper.count(studentFilter, status), page, size);
    }

    @Transactional
    public WithdrawalDtos.WithdrawalView review(Long withdrawalId, WithdrawalDtos.ReviewRequest request) {
        currentUserService.requireAnyRole("ACADEMIC");
        WithdrawalRequest snapshot = requireWithdrawal(withdrawalId);
        Enrollment enrollment = enrollmentMapper.findByIdForUpdate(snapshot.getEnrollmentId());
        if (enrollment == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "报名记录不存在");
        }
        WithdrawalRequest withdrawal = requireWithdrawalForUpdate(withdrawalId);
        if (!"SUBMITTED".equals(withdrawal.getStatus())) {
            throw new BusinessException(HttpStatus.CONFLICT, "只有待审核退学申请可以审核");
        }
        BigDecimal amount = null;
        String status = "REJECTED";
        if (request.approved()) {
            if (request.approvedRefundAmount() == null) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "审核通过必须核定额度");
            }
            if (request.approvedRefundAmount().compareTo(enrollment.getPaidAmount()) > 0) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "退款额度不能超过实缴金额");
            }
            amount = request.approvedRefundAmount();
            status = "APPROVED";
        }
        if (withdrawalMapper.review(withdrawalId, status, currentUserService.userId(),
                LocalDateTime.now(), request.reviewNote(), amount) != 1) {
            throw new BusinessException(HttpStatus.CONFLICT, "退学申请状态已变化，请刷新后重试");
        }
        return toView(withdrawalMapper.findById(withdrawalId), enrollment.getStudentUserId());
    }

    @Transactional
    public WithdrawalDtos.WithdrawalView refund(Long withdrawalId, WithdrawalDtos.RefundRequest request) {
        currentUserService.requireAnyRole("FINANCE");
        WithdrawalRequest snapshot = requireWithdrawal(withdrawalId);
        Enrollment enrollment = enrollmentMapper.findByIdForUpdate(snapshot.getEnrollmentId());
        if (enrollment == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "报名记录不存在");
        }
        WithdrawalRequest withdrawal = requireWithdrawalForUpdate(withdrawalId);
        if ("REFUNDED".equals(withdrawal.getStatus())) {
            return toView(withdrawal, enrollment.getStudentUserId());
        }
        if (!"APPROVED".equals(withdrawal.getStatus())) {
            throw new BusinessException(HttpStatus.CONFLICT, "只有已核定申请可以登记退款");
        }
        if (withdrawal.getApprovedRefundAmount() == null) {
            throw new BusinessException(HttpStatus.CONFLICT, "退款额度未核定");
        }
        if (withdrawalMapper.refund(withdrawalId, withdrawal.getApprovedRefundAmount(),
                request.refundMethod(), request.refundVoucher(), currentUserService.userId(),
                LocalDateTime.now()) != 1) {
            throw new BusinessException(HttpStatus.CONFLICT, "退款状态已变化，请刷新后重试");
        }
        if (enrollmentMapper.markWithdrawn(enrollment.getEnrollmentId()) != 1) {
            throw new BusinessException(HttpStatus.CONFLICT, "报名状态不允许退学结算");
        }
        return toView(withdrawalMapper.findById(withdrawalId), enrollment.getStudentUserId());
    }

    private WithdrawalRequest requireWithdrawal(Long withdrawalId) {
        WithdrawalRequest withdrawal = withdrawalMapper.findById(withdrawalId);
        if (withdrawal == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "退学申请不存在");
        }
        return withdrawal;
    }

    private WithdrawalRequest requireWithdrawalForUpdate(Long withdrawalId) {
        WithdrawalRequest withdrawal = withdrawalMapper.findByIdForUpdate(withdrawalId);
        if (withdrawal == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "退学申请不存在");
        }
        return withdrawal;
    }

    private Enrollment requireEnrollment(Long enrollmentId) {
        Enrollment enrollment = enrollmentMapper.findById(enrollmentId);
        if (enrollment == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "报名记录不存在");
        }
        return enrollment;
    }

    private void requireCanView(Enrollment enrollment, WithdrawalRequest withdrawal) {
        LoginUser user = currentUserService.requireUser();
        boolean student = user.roleCodes().contains("STUDENT") && user.userId().equals(enrollment.getStudentUserId());
        boolean staff = user.roleCodes().stream().anyMatch(List.of("ACADEMIC", "FINANCE", "ADMIN")::contains);
        if (!student && !staff) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "无权查看该退学退费记录");
        }
    }

    private WithdrawalDtos.WithdrawalView toView(WithdrawalRequest w, Long studentUserId) {
        return new WithdrawalDtos.WithdrawalView(String.valueOf(w.getWithdrawalId()),
                String.valueOf(w.getEnrollmentId()), String.valueOf(studentUserId), w.getReason(), w.getRequestedAt(),
                w.getStatus(), stringId(w.getReviewedBy()), w.getReviewedAt(), w.getReviewNote(),
                w.getApprovedRefundAmount(), w.getRefundAmount(), w.getRefundMethod(), w.getRefundVoucher(),
                stringId(w.getRefundedBy()), w.getRefundedAt());
    }

    private String stringId(Long id) {
        return id == null ? null : String.valueOf(id);
    }
}
