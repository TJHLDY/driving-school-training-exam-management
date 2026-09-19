package com.example.drivingschool.service;

import com.example.drivingschool.common.PageResult;
import com.example.drivingschool.common.PageUtils;
import com.example.drivingschool.config.EnrollmentProperties;
import com.example.drivingschool.dto.EnrollmentDtos;
import com.example.drivingschool.entity.Enrollment;
import com.example.drivingschool.exception.BusinessException;
import com.example.drivingschool.mapper.EnrollmentMapper;
import com.example.drivingschool.security.LoginUser;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class EnrollmentService {
    private final EnrollmentMapper enrollmentMapper;
    private final CurrentUserService currentUserService;
    private final EnrollmentProperties properties;

    public EnrollmentService(EnrollmentMapper enrollmentMapper, CurrentUserService currentUserService,
                             EnrollmentProperties properties) {
        this.enrollmentMapper = enrollmentMapper;
        this.currentUserService = currentUserService;
        this.properties = properties;
    }

    @Transactional
    public EnrollmentDtos.EnrollmentView submit(EnrollmentDtos.SubmitRequest request) {
        LoginUser user = currentUserService.requireUser();
        currentUserService.requireAnyRole("STUDENT");
        Enrollment existing = enrollmentMapper.findByStudentUserIdForUpdate(user.userId());
        LocalDateTime now = LocalDateTime.now();
        BigDecimal defaultAmount = properties.defaultAmount(request.licenseType());
        if (existing == null) {
            Enrollment enrollment = new Enrollment();
            enrollment.setStudentUserId(user.userId());
            enrollment.setLicenseType(request.licenseType());
            enrollment.setRequiredAmount(defaultAmount);
            enrollment.setPlannedHours(properties.plannedHours());
            enrollment.setSubmittedAt(now);
            enrollmentMapper.insert(enrollment);
            return toView(enrollmentMapper.findById(enrollment.getEnrollmentId()));
        }
        if (!List.of("SUBMITTED", "REJECTED").contains(existing.getStatus())) {
            throw new BusinessException(HttpStatus.CONFLICT, "当前报名状态不允许修改");
        }
        existing.setLicenseType(request.licenseType());
        existing.setRequiredAmount(defaultAmount);
        existing.setPlannedHours(properties.plannedHours());
        existing.setSubmittedAt(now);
        if (enrollmentMapper.updateStudentSubmission(existing) != 1) {
            throw new BusinessException(HttpStatus.CONFLICT, "报名状态已变化，请刷新后重试");
        }
        return toView(enrollmentMapper.findById(existing.getEnrollmentId()));
    }

    public EnrollmentDtos.EnrollmentView get(Long enrollmentId) {
        Enrollment enrollment = requireEnrollment(enrollmentId);
        requireCanView(enrollment);
        return toView(enrollment);
    }

    public PageResult<EnrollmentDtos.EnrollmentView> list(String status, Integer pageNo, Integer pageSize) {
        LoginUser user = currentUserService.requireUser();
        Long studentFilter = null;
        if (user.roleCodes().contains("STUDENT")) {
            studentFilter = user.userId();
        } else if (!user.roleCodes().stream().anyMatch(List.of("ACADEMIC", "FINANCE", "ADMIN")::contains)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "无权查询报名记录");
        }
        int page = PageUtils.page(pageNo);
        int size = PageUtils.pageSize(pageSize);
        long offset = PageUtils.offset(page, size);
        List<EnrollmentDtos.EnrollmentView> list = enrollmentMapper.findPage(studentFilter, status, offset, size)
                .stream().map(this::toView).toList();
        return new PageResult<>(list, enrollmentMapper.count(studentFilter, status), page, size);
    }

    @Transactional
    public EnrollmentDtos.EnrollmentView review(Long enrollmentId, EnrollmentDtos.ReviewRequest request) {
        currentUserService.requireAnyRole("ACADEMIC");
        Enrollment enrollment = requireEnrollmentForUpdate(enrollmentId);
        if (!"SUBMITTED".equals(enrollment.getStatus())) {
            throw new BusinessException(HttpStatus.CONFLICT, "只有待审核报名可以审核");
        }
        String status;
        BigDecimal amount = enrollment.getRequiredAmount();
        BigDecimal hours = enrollment.getPlannedHours();
        if (request.approved()) {
            if (request.requiredAmount() == null || request.plannedHours() == null) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "审核通过必须确认费用和学时");
            }
            amount = request.requiredAmount();
            hours = request.plannedHours();
            status = "APPROVED";
        } else {
            status = "REJECTED";
        }
        int changed = enrollmentMapper.review(enrollmentId, status, amount, hours,
                currentUserService.userId(), LocalDateTime.now(), request.reviewNote());
        if (changed != 1) {
            throw new BusinessException(HttpStatus.CONFLICT, "报名状态已变化，请刷新后重试");
        }
        return toView(enrollmentMapper.findById(enrollmentId));
    }

    @Transactional
    public EnrollmentDtos.EnrollmentView pay(Long enrollmentId, EnrollmentDtos.PaymentRequest request) {
        currentUserService.requireAnyRole("FINANCE");
        Enrollment enrollment = requireEnrollmentForUpdate(enrollmentId);
        if ("ACTIVE".equals(enrollment.getStatus()) && enrollment.getPaidAt() != null) {
        }
        if (!"APPROVED".equals(enrollment.getStatus())) {
            throw new BusinessException(HttpStatus.CONFLICT, "只有审核通过的报名可以缴费");
        }
        int changed = enrollmentMapper.pay(enrollmentId, enrollment.getRequiredAmount(), request.paymentMethod(),
                request.paymentVoucher(), currentUserService.userId(), LocalDateTime.now());
        if (changed != 1) {
            throw new BusinessException(HttpStatus.CONFLICT, "缴费状态已变化，请刷新后重试");
        }
        return toView(enrollmentMapper.findById(enrollmentId));
    }

    private Enrollment requireEnrollment(Long enrollmentId) {
        Enrollment enrollment = enrollmentMapper.findById(enrollmentId);
        if (enrollment == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "报名记录不存在");
        }
        return enrollment;
    }

    private Enrollment requireEnrollmentForUpdate(Long enrollmentId) {
        Enrollment enrollment = enrollmentMapper.findByIdForUpdate(enrollmentId);
        if (enrollment == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "报名记录不存在");
        }
        return enrollment;
    }

    private void requireCanView(Enrollment enrollment) {
        LoginUser user = currentUserService.requireUser();
        if (user.roleCodes().contains("STUDENT") && !user.userId().equals(enrollment.getStudentUserId())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "不能查看他人报名记录");
        }
        if (user.roleCodes().stream().noneMatch(List.of("STUDENT", "ACADEMIC", "FINANCE", "ADMIN")::contains)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "无权查看报名记录");
        }
    }

    private EnrollmentDtos.EnrollmentView toView(Enrollment e) {
        return new EnrollmentDtos.EnrollmentView(String.valueOf(e.getEnrollmentId()), String.valueOf(e.getStudentUserId()),
                e.getLicenseType(), e.getRequiredAmount(), e.getPlannedHours(), e.getStatus(), e.getSubmittedAt(),
                stringId(e.getReviewedBy()), e.getReviewedAt(), e.getReviewNote(), e.getPaidAmount(),
                e.getPaymentMethod(), e.getPaymentVoucher(), stringId(e.getPaidBy()), e.getPaidAt());
    }

    private String stringId(Long id) {
        return id == null ? null : String.valueOf(id);
    }
}
