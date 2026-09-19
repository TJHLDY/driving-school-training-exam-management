package com.example.drivingschool.service;

import com.example.drivingschool.common.PageResult;
import com.example.drivingschool.common.PageUtils;
import com.example.drivingschool.dto.TrainingDtos;
import com.example.drivingschool.entity.Enrollment;
import com.example.drivingschool.entity.TrainingBooking;
import com.example.drivingschool.entity.TrainingVehicle;
import com.example.drivingschool.exception.BusinessException;
import com.example.drivingschool.mapper.AccountMapper;
import com.example.drivingschool.mapper.EnrollmentMapper;
import com.example.drivingschool.mapper.TrainingMapper;
import com.example.drivingschool.mapper.WithdrawalMapper;
import com.example.drivingschool.security.LoginUser;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class TrainingService {
    private final TrainingMapper trainingMapper;
    private final AccountMapper accountMapper;
    private final EnrollmentMapper enrollmentMapper;
    private final WithdrawalMapper withdrawalMapper;
    private final CurrentUserService currentUserService;

    public TrainingService(TrainingMapper trainingMapper, AccountMapper accountMapper,
                           EnrollmentMapper enrollmentMapper, WithdrawalMapper withdrawalMapper,
                           CurrentUserService currentUserService) {
        this.trainingMapper = trainingMapper;
        this.accountMapper = accountMapper;
        this.enrollmentMapper = enrollmentMapper;
        this.withdrawalMapper = withdrawalMapper;
        this.currentUserService = currentUserService;
    }

    public List<TrainingDtos.VehicleView> listVehicles(Boolean enabled) {
        currentUserService.requireUser();
        return trainingMapper.findVehicles(enabled).stream().map(this::toVehicleView).toList();
    }

    @Transactional
    public TrainingDtos.VehicleView createVehicle(TrainingDtos.VehicleRequest request) {
        currentUserService.requireAnyRole("ACADEMIC");
        TrainingVehicle vehicle = new TrainingVehicle();
        vehicle.setPlateNo(request.plateNo().trim());
        vehicle.setLicenseType(request.licenseType());
        vehicle.setEnabled(request.enabled());
        trainingMapper.insertVehicle(vehicle);
        return toVehicleView(vehicle);
    }

    @Transactional
    public TrainingDtos.VehicleView updateVehicle(Long vehicleId, TrainingDtos.VehicleRequest request) {
        currentUserService.requireAnyRole("ACADEMIC");
        TrainingVehicle vehicle = trainingMapper.findVehicleByIdForUpdate(vehicleId);
        if (vehicle == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "车辆不存在");
        }
        vehicle.setPlateNo(request.plateNo().trim());
        vehicle.setLicenseType(request.licenseType());
        vehicle.setEnabled(request.enabled());
        trainingMapper.updateVehicle(vehicle);
        return toVehicleView(vehicle);
    }

    @Transactional
    public TrainingDtos.BookingView createOpenBooking(TrainingDtos.OpenBookingRequest request) {
        Long actorId = currentUserService.userId();
        currentUserService.requireAnyRole("ACADEMIC");
        requireValidPeriod(request.plannedStart(), request.plannedEnd());
        if (request.plannedStart().isBefore(LocalDateTime.now())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "不能创建已开始的培训时段");
        }
        TrainingBooking booking = new TrainingBooking();
        booking.setPlannedStart(request.plannedStart());
        booking.setPlannedEnd(request.plannedEnd());
        booking.setLocation(request.location().trim());
        booking.setCreatedBy(actorId);
        trainingMapper.insertOpenBooking(booking);
        return toBookingView(trainingMapper.findBookingById(booking.getBookingId()));
    }

    @Transactional
    public TrainingDtos.BookingView claim(Long bookingId) {
        LoginUser user = currentUserService.requireUser();
        currentUserService.requireAnyRole("STUDENT");
        accountMapper.findByIdForUpdate(user.userId());
        TrainingBooking booking = requireBookingForUpdate(bookingId);
        if (!"OPEN".equals(booking.getStatus()) || booking.getStudentUserId() != null) {
            throw new BusinessException(HttpStatus.CONFLICT, "该时段已被认领或已关闭");
        }
        if (booking.getPlannedStart().isBefore(LocalDateTime.now())) {
            throw new BusinessException(HttpStatus.CONFLICT, "已开始的时段不能预约");
        }
        Enrollment enrollment = enrollmentMapper.findByStudentUserIdForUpdate(user.userId());
        if (enrollment == null || !"ACTIVE".equals(enrollment.getStatus())) {
            throw new BusinessException(HttpStatus.CONFLICT, "只有已缴费生效的学员可以预约");
        }
        var withdrawal = withdrawalMapper.findByEnrollmentId(enrollment.getEnrollmentId());
        if (withdrawal != null && List.of("SUBMITTED", "APPROVED").contains(withdrawal.getStatus())) {
            throw new BusinessException(HttpStatus.CONFLICT, "退学处理中，不能新增预约");
        }
        if (trainingMapper.countOverlaps(user.userId(), null, null,
                booking.getPlannedStart(), booking.getPlannedEnd(), bookingId) > 0) {
            throw new BusinessException(HttpStatus.CONFLICT, "学员在该时段已有培训");
        }
        if (trainingMapper.claim(bookingId, user.userId(), LocalDateTime.now()) != 1) {
            throw new BusinessException(HttpStatus.CONFLICT, "该时段已被其他学员认领");
        }
        return toBookingView(trainingMapper.findBookingById(bookingId));
    }

    @Transactional
    public TrainingDtos.BookingView assign(Long bookingId, TrainingDtos.AssignRequest request) {
        Long actorId = currentUserService.userId();
        currentUserService.requireAnyRole("ACADEMIC");
        TrainingBooking booking = requireBookingForUpdate(bookingId);
        if (!"PENDING".equals(booking.getStatus())) {
            throw new BusinessException(HttpStatus.CONFLICT, "只有待分配预约可以指定资源");
        }
        accountMapper.findByIdForUpdate(booking.getStudentUserId());
        var coach = accountMapper.findByIdForUpdate(request.coachUserId());
        if (coach == null || !Boolean.TRUE.equals(coach.getEnabled())
                || !accountMapper.findRoleCodesByUserId(coach.getUserId()).contains("COACH")) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "指定用户不是可用教练");
        }
        TrainingVehicle vehicle = trainingMapper.findVehicleByIdForUpdate(request.vehicleId());
        if (vehicle == null || !Boolean.TRUE.equals(vehicle.getEnabled())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "指定车辆不存在或已停用");
        }
        Enrollment enrollment = enrollmentMapper.findByStudentUserId(booking.getStudentUserId());
        if (enrollment == null || !vehicle.getLicenseType().equals(enrollment.getLicenseType())) {
            throw new BusinessException(HttpStatus.CONFLICT, "车辆车型与报名车型不一致");
        }
        if (trainingMapper.countOverlaps(booking.getStudentUserId(), coach.getUserId(), vehicle.getVehicleId(),
                booking.getPlannedStart(), booking.getPlannedEnd(), bookingId) > 0) {
            throw new BusinessException(HttpStatus.CONFLICT, "学员、教练或车辆存在时间冲突");
        }
        if (trainingMapper.assign(bookingId, coach.getUserId(), vehicle.getVehicleId(), actorId,
                LocalDateTime.now()) != 1) {
            throw new BusinessException(HttpStatus.CONFLICT, "预约状态已变化，请刷新后重试");
        }
        return toBookingView(trainingMapper.findBookingById(bookingId));
    }

    @Transactional
    public TrainingDtos.BookingView cancel(Long bookingId) {
        LoginUser user = currentUserService.requireUser();
        TrainingBooking booking = requireBookingForUpdate(bookingId);
        if (!List.of("PENDING", "ASSIGNED").contains(booking.getStatus())) {
            throw new BusinessException(HttpStatus.CONFLICT, "当前预约状态不能取消");
        }
        if (!booking.getPlannedStart().isAfter(LocalDateTime.now())) {
            throw new BusinessException(HttpStatus.CONFLICT, "培训开始后不能取消");
        }
        boolean studentOwner = user.roleCodes().contains("STUDENT") && user.userId().equals(booking.getStudentUserId());
        boolean academic = user.roleCodes().contains("ACADEMIC");
        if (!studentOwner && !academic) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "只能取消本人预约或由教务取消");
        }
        if (trainingMapper.cancel(bookingId, LocalDateTime.now()) != 1) {
            throw new BusinessException(HttpStatus.CONFLICT, "预约状态已变化，请刷新后重试");
        }
        return toBookingView(trainingMapper.findBookingById(bookingId));
    }

    @Transactional
    public TrainingDtos.BookingView complete(Long bookingId, TrainingDtos.CompleteRequest request) {
        LoginUser coach = currentUserService.requireUser();
        currentUserService.requireAnyRole("COACH");
        TrainingBooking booking = requireBookingForUpdate(bookingId);
        if (!"ASSIGNED".equals(booking.getStatus())) {
            throw new BusinessException(HttpStatus.CONFLICT, "只有已分配预约可以登记结果");
        }
        if (!coach.userId().equals(booking.getCoachUserId())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "教练只能登记分给自己的培训");
        }
        requireValidPeriod(request.actualStart(), request.actualEnd());
        BigDecimal hours = request.validHours().setScale(2, RoundingMode.HALF_UP);
        BigDecimal durationHours = BigDecimal.valueOf(Duration.between(request.actualStart(), request.actualEnd()).toMinutes())
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
        if (hours.compareTo(durationHours) > 0) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "有效学时不能超过实际培训时长");
        }
        if (trainingMapper.complete(bookingId, request.actualStart(), request.actualEnd(), hours,
                request.resultNote()) != 1) {
            throw new BusinessException(HttpStatus.CONFLICT, "预约状态已变化，请刷新后重试");
        }
        return toBookingView(trainingMapper.findBookingById(bookingId));
    }

    public TrainingDtos.BookingView get(Long bookingId) {
        TrainingBooking booking = trainingMapper.findBookingById(bookingId);
        if (booking == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "培训预约不存在");
        }
        requireCanView(booking);
        return toBookingView(booking);
    }

    public PageResult<TrainingDtos.BookingView> list(String status, Integer pageNo, Integer pageSize) {
        LoginUser user = currentUserService.requireUser();
        Long studentFilter = user.roleCodes().contains("STUDENT") ? user.userId() : null;
        Long coachFilter = user.roleCodes().contains("COACH") && studentFilter == null ? user.userId() : null;
        if (studentFilter == null && coachFilter == null
                && user.roleCodes().stream().noneMatch(List.of("ACADEMIC", "ADMIN")::contains)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "无权查询培训记录");
        }
        int page = PageUtils.page(pageNo);
        int size = PageUtils.pageSize(pageSize);
        long offset = PageUtils.offset(page, size);
        List<TrainingDtos.BookingView> list = trainingMapper.findPage(studentFilter, coachFilter, status, offset, size)
                .stream().map(this::toBookingView).toList();
        return new PageResult<>(list, trainingMapper.count(studentFilter, coachFilter, status), page, size);
    }

    public TrainingDtos.TrainingHoursView completedHours(Long studentUserId) {
        LoginUser user = currentUserService.requireUser();
        boolean self = user.userId().equals(studentUserId) && user.roleCodes().contains("STUDENT");
        boolean staff = user.roleCodes().stream().anyMatch(List.of("ACADEMIC", "ADMIN")::contains);
        boolean coachRelation = user.roleCodes().contains("COACH")
                && trainingMapper.count(studentUserId, user.userId(), "COMPLETED") > 0;
        if (!self && !staff && !coachRelation) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "无权查看该学员累计学时");
        }
        BigDecimal hours = trainingMapper.sumCompletedHours(studentUserId);
        return new TrainingDtos.TrainingHoursView(String.valueOf(studentUserId), hours);
    }

    private TrainingBooking requireBookingForUpdate(Long bookingId) {
        TrainingBooking booking = trainingMapper.findBookingByIdForUpdate(bookingId);
        if (booking == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "培训预约不存在");
        }
        return booking;
    }

    private void requireCanView(TrainingBooking booking) {
        LoginUser user = currentUserService.requireUser();
        boolean student = user.roleCodes().contains("STUDENT") && user.userId().equals(booking.getStudentUserId());
        boolean coach = user.roleCodes().contains("COACH") && user.userId().equals(booking.getCoachUserId());
        boolean staff = user.roleCodes().stream().anyMatch(List.of("ACADEMIC", "ADMIN")::contains);
        if (!student && !coach && !staff) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "无权查看该培训记录");
        }
    }

    private void requireValidPeriod(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null || !end.isAfter(start)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "结束时间必须晚于开始时间");
        }
    }

    private TrainingDtos.VehicleView toVehicleView(TrainingVehicle vehicle) {
        return new TrainingDtos.VehicleView(String.valueOf(vehicle.getVehicleId()), vehicle.getPlateNo(),
                vehicle.getLicenseType(), vehicle.getEnabled());
    }

    private TrainingDtos.BookingView toBookingView(TrainingBooking b) {
        return new TrainingDtos.BookingView(stringId(b.getBookingId()), stringId(b.getStudentUserId()),
                stringId(b.getCoachUserId()), stringId(b.getVehicleId()), b.getPlannedStart(), b.getPlannedEnd(),
                b.getLocation(), b.getStatus(), stringId(b.getCreatedBy()), b.getRequestedAt(),
                stringId(b.getAssignedBy()), b.getAssignedAt(), b.getActualStart(), b.getActualEnd(),
                b.getValidHours(), b.getResultNote(), b.getCancelledAt());
    }

    private String stringId(Long id) {
        return id == null ? null : String.valueOf(id);
    }
}
