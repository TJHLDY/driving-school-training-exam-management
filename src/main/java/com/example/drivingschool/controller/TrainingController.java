package com.example.drivingschool.controller;
import com.example.drivingschool.config.SessionUser;
import com.example.drivingschool.entity.Enrollment;
import com.example.drivingschool.entity.TrainingBooking;
import com.example.drivingschool.entity.TrainingVehicle;
import com.example.drivingschool.mapper.EnrollmentMapper;
import com.example.drivingschool.mapper.TrainingBookingMapper;
import com.example.drivingschool.mapper.TrainingVehicleMapper;
import com.example.drivingschool.mapper.UserAccountMapper;
import com.example.drivingschool.mapper.WithdrawalRequestMapper;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
@Controller

public class TrainingController {
    @Autowired private TrainingBookingMapper bookingMapper;
    @Autowired private TrainingVehicleMapper vehicleMapper;
    @Autowired private EnrollmentMapper enrollmentMapper;
    @Autowired private WithdrawalRequestMapper withdrawalMapper;
    @Autowired private UserAccountMapper userAccountMapper;
    private SessionUser current(HttpSession session) {
        return (SessionUser) session.getAttribute("loginUser");
    }
    @GetMapping("/training")
    public String list(HttpSession session, Model model) {
        SessionUser user = current(session);
        boolean academic = user.hasRole("ACADEMIC");
        boolean coach = user.hasRole("COACH") && !academic;
        List<TrainingBooking> openSlots = bookingMapper.selectOpenSlots(LocalDateTime.now());
        List<com.example.drivingschool.dto.TrainingBookingDetailDto> bookings;
        if (academic) {
            bookings = bookingMapper.selectDetailList(null, null, null);
        } else if (coach) {
            bookings = bookingMapper.selectDetailList(null, user.getUserId(), null);
        } else {
            bookings = bookingMapper.selectDetailList(user.getUserId(), null, null);
        }
        model.addAttribute("openSlots", openSlots);
        model.addAttribute("bookings", bookings);
        model.addAttribute("canManage", academic);
        model.addAttribute("canBook", user.hasRole("STUDENT"));
        model.addAttribute("canRecord", user.hasRole("COACH"));
        model.addAttribute("vehicles", user.hasRole("STUDENT")
                ? List.<TrainingVehicle>of() : vehicleMapper.selectList(true, null));
        model.addAttribute("coachList", user.hasRole("STUDENT")
                ? List.of() : coachUsers());
        model.addAttribute("totalHours", bookingMapper.sumValidHoursByStudentUserId(user.getUserId()));
        return "training/list";
    }
    // 有 COACH 角色的账号才出现在教练下拉框里
    private List<com.example.drivingschool.entity.UserAccount> coachUsers() {
        return userAccountMapper.selectList(null, true).stream()
                .filter(u -> u.getCoachLicense() != null && !u.getCoachLicense().isBlank())
                .toList();
    }
    @PostMapping("/training/slots")
    public String createSlot(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime plannedStart,
                             @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime plannedEnd,
                             @RequestParam String location,
                             HttpSession session, RedirectAttributes ra) {
        SessionUser user = current(session);
        if (!user.hasRole("ACADEMIC")) {
            ra.addFlashAttribute("err", "只有教务可以发布培训时段");
            return "redirect:/training";
        }
        if (!plannedEnd.isAfter(plannedStart)) {
            ra.addFlashAttribute("err", "结束时间必须晚于开始时间");
            return "redirect:/training";
        }
        TrainingBooking slot = new TrainingBooking();
        slot.setPlannedStart(plannedStart);
        slot.setPlannedEnd(plannedEnd);
        slot.setLocation(location);
        slot.setCreatedBy(user.getUserId());
        bookingMapper.insertOpenSlot(slot);
        ra.addFlashAttribute("msg", "时段已发布，学员可以认领");
        return "redirect:/training";
    }
    // 学员认领时段：条件更新 OPEN，抢先失败会返回 0
    @PostMapping("/training/{id}/claim")
    public String claim(@PathVariable Long id, HttpSession session, RedirectAttributes ra) {
        SessionUser user = current(session);
        if (!user.hasRole("STUDENT")) {
            ra.addFlashAttribute("err", "只有学员可以认领时段");
            return "redirect:/training";
        }
        Enrollment enrollment = enrollmentMapper.selectByStudentUserId(user.getUserId());
        if (enrollment == null || !"ACTIVE".equals(enrollment.getStatus())) {
            ra.addFlashAttribute("err", "报名生效后才能预约培训");
            return "redirect:/training";
        }
        if (withdrawalMapper.countProcessingByStudentUserId(user.getUserId()) > 0) {
            ra.addFlashAttribute("err", "有处理中的退学申请，不能新增预约");
            return "redirect:/training";
        }
        TrainingBooking slot = bookingMapper.selectByIdForUpdate(id);
        if (slot == null || !"OPEN".equals(slot.getStatus())
                || !slot.getPlannedStart().isAfter(LocalDateTime.now())) {
            ra.addFlashAttribute("err", "该时段不可预约");
            return "redirect:/training";
        }
        if (bookingMapper.countStudentConflict(user.getUserId(),
                slot.getPlannedStart(), slot.getPlannedEnd()) > 0) {
            ra.addFlashAttribute("err", "你在这个时间段已有培训安排");
            return "redirect:/training";
        }
        int rows = bookingMapper.claimOpenSlot(id, user.getUserId());
        ra.addFlashAttribute(rows > 0 ? "msg" : "err",
                rows > 0 ? "预约成功，等待教务分配教练和车辆" : "这个时段已经被别人抢先预约了");
        return "redirect:/training";
    }
    @PostMapping("/training/{id}/assign")
    public String assign(@PathVariable Long id, @RequestParam Long coachUserId,
                         @RequestParam Long vehicleId, HttpSession session, RedirectAttributes ra) {
        SessionUser user = current(session);
        if (!user.hasRole("ACADEMIC")) {
            ra.addFlashAttribute("err", "只有教务可以分配教练和车辆");
            return "redirect:/training";
        }
        TrainingBooking booking = bookingMapper.selectById(id);
        if (booking == null || !"PENDING".equals(booking.getStatus())) {
            ra.addFlashAttribute("err", "只有待分配的预约才能分配资源");
            return "redirect:/training";
        }
        Enrollment enrollment = enrollmentMapper.selectByStudentUserId(booking.getStudentUserId());
        TrainingVehicle vehicle = vehicleMapper.selectById(vehicleId);
        if (vehicle == null || Boolean.FALSE.equals(vehicle.getEnabled())) {
            ra.addFlashAttribute("err", "车辆不可用");
            return "redirect:/training";
        }
        // 车辆车型必须和学员报名的驾照类型一致
        if (enrollment != null && !vehicle.getLicenseType().equals(enrollment.getLicenseType())) {
            ra.addFlashAttribute("err", "车辆车型（" + vehicle.getLicenseType()
                    + "）与学员报名类型（" + enrollment.getLicenseType() + "）不一致");
            return "redirect:/training";
        }
        if (bookingMapper.countCoachConflict(coachUserId, booking.getPlannedStart(), booking.getPlannedEnd()) > 0) {
            ra.addFlashAttribute("err", "该教练在这个时间段已有安排");
            return "redirect:/training";
        }
        if (bookingMapper.countVehicleConflict(vehicleId, booking.getPlannedStart(), booking.getPlannedEnd()) > 0) {
            ra.addFlashAttribute("err", "该车辆在这个时间段已被占用");
            return "redirect:/training";
        }
        int rows = bookingMapper.assign(id, coachUserId, vehicleId, user.getUserId());
        ra.addFlashAttribute(rows > 0 ? "msg" : "err", rows > 0 ? "分配完成" : "分配失败，请刷新重试");
        return "redirect:/training";
    }
    // 教练登记实际培训结果，有效学时不能超过实际时长
    @PostMapping("/training/{id}/record")
    public String record(@PathVariable Long id,
                         @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime actualStart,
                         @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime actualEnd,
                         @RequestParam BigDecimal validHours,
                         @RequestParam(required = false) String resultNote,
                         HttpSession session, RedirectAttributes ra) {
        SessionUser user = current(session);
        TrainingBooking booking = bookingMapper.selectById(id);
        if (booking == null || !user.getUserId().equals(booking.getCoachUserId())) {
            ra.addFlashAttribute("err", "只能登记分配给自己负责的培训");
            return "redirect:/training";
        }
        if (!"ASSIGNED".equals(booking.getStatus())) {
            ra.addFlashAttribute("err", "该预约当前状态不能登记结果");
            return "redirect:/training";
        }
        if (!actualEnd.isAfter(actualStart)) {
            ra.addFlashAttribute("err", "结束时间必须晚于开始时间");
            return "redirect:/training";
        }
        long seconds = Duration.between(actualStart, actualEnd).getSeconds();
        if (validHours == null || validHours.compareTo(BigDecimal.ZERO) < 0
                || validHours.multiply(new BigDecimal("3600")).compareTo(new BigDecimal(seconds)) > 0) {
            ra.addFlashAttribute("err", "有效学时不能为负，也不能超过实际训练时长");
            return "redirect:/training";
        }
        int rows = bookingMapper.complete(id, actualStart, actualEnd, validHours, resultNote);
        ra.addFlashAttribute(rows > 0 ? "msg" : "err",
                rows > 0 ? "培训结果已登记，有效学时 " + validHours + " 小时" : "登记失败，请刷新重试");
        return "redirect:/training";
    }
    @Transactional
    @PostMapping("/training/{id}/cancel")
    public String cancel(@PathVariable Long id, HttpSession session, RedirectAttributes ra) {
        SessionUser user = current(session);
        TrainingBooking booking = bookingMapper.selectById(id);
        if (booking == null) {
            ra.addFlashAttribute("err", "预约不存在");
            return "redirect:/training";
        }
        boolean owner = booking.getStudentUserId() != null
                && booking.getStudentUserId().equals(user.getUserId());
        if (!owner && !user.hasRole("ACADEMIC")) {
            ra.addFlashAttribute("err", "只能取消本人的预约");
            return "redirect:/training";
        }
        if (!booking.getPlannedStart().isAfter(LocalDateTime.now())) {
            ra.addFlashAttribute("err", "培训已开始，不能取消");
            return "redirect:/training";
        }
        int rows = bookingMapper.cancel(id);
        ra.addFlashAttribute(rows > 0 ? "msg" : "err",
                rows > 0 ? "预约已取消" : "当前状态不能取消");
        return "redirect:/training";
    }
}
