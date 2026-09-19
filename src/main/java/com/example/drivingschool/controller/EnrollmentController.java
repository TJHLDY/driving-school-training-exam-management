package com.example.drivingschool.controller;
import com.example.drivingschool.config.SessionUser;
import com.example.drivingschool.entity.Enrollment;
import com.example.drivingschool.mapper.EnrollmentMapper;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.math.BigDecimal;
import java.util.Map;
@Controller

public class EnrollmentController {
    // 报名时先按车型写入约定金额和学时，教务审核时可以调整，学员不能自己改价
    private static final Map<String, BigDecimal[]> DEFAULTS = Map.of(
            "C1", new BigDecimal[]{new BigDecimal("4800"), new BigDecimal("40")},
            "C2", new BigDecimal[]{new BigDecimal("5000"), new BigDecimal("40")});
    @Autowired
    private EnrollmentMapper enrollmentMapper;
    private SessionUser current(HttpSession session) {
        return (SessionUser) session.getAttribute("loginUser");
    }
    @GetMapping("/enrollments")
    public String list(@RequestParam(required = false) String status,
                       HttpSession session, Model model) {
        SessionUser user = current(session);
        Long studentUserId = user.isOnlyRole("STUDENT") ? user.getUserId() : null;
        model.addAttribute("enrollments", enrollmentMapper.selectDetailList(studentUserId, status, null));
        model.addAttribute("status", status);
        model.addAttribute("canReview", user.hasRole("ACADEMIC"));
        model.addAttribute("canPay", user.hasRole("FINANCE"));
        model.addAttribute("canApply", user.hasRole("STUDENT"));
        return "enrollment/list";
    }
    @GetMapping("/enrollments/new")
    public String createForm(HttpSession session) {
        if (!current(session).hasRole("STUDENT")) {
            return "redirect:/enrollments";
        }
        return "enrollment/form";
    }
    @PostMapping("/enrollments")
    public String create(@RequestParam String licenseType, HttpSession session,
                         RedirectAttributes ra) {
        SessionUser user = current(session);
        if (!user.hasRole("STUDENT")) {
            ra.addFlashAttribute("err", "只有学员可以提交报名");
            return "redirect:/enrollments";
        }
        if (enrollmentMapper.selectByStudentUserId(user.getUserId()) != null) {
            ra.addFlashAttribute("err", "你已经有报名记录，不能重复提交");
            return "redirect:/enrollments";
        }
        BigDecimal[] defaults = DEFAULTS.getOrDefault(licenseType, DEFAULTS.get("C1"));
        Enrollment enrollment = new Enrollment();
        enrollment.setStudentUserId(user.getUserId());
        enrollment.setLicenseType(licenseType);
        enrollment.setRequiredAmount(defaults[0]);
        enrollment.setPlannedHours(defaults[1]);
        enrollment.setStatus("SUBMITTED");
        enrollmentMapper.insert(enrollment);
        ra.addFlashAttribute("msg", "报名已提交，等待教务审核");
        return "redirect:/enrollments";
    }
    // 驳回后修改原记录重新提交，不新建第二条报名
    @PostMapping("/enrollments/{id}/resubmit")
    public String resubmit(@PathVariable Long id, @RequestParam String licenseType,
                           HttpSession session, RedirectAttributes ra) {
        SessionUser user = current(session);
        Enrollment enrollment = enrollmentMapper.selectById(id);
        if (enrollment == null || !enrollment.getStudentUserId().equals(user.getUserId())) {
            ra.addFlashAttribute("err", "只能修改本人的报名");
            return "redirect:/enrollments";
        }
        BigDecimal[] defaults = DEFAULTS.getOrDefault(licenseType, DEFAULTS.get("C1"));
        int rows = enrollmentMapper.updateAndResubmit(id, licenseType, defaults[0]);
        ra.addFlashAttribute(rows > 0 ? "msg" : "err",
                rows > 0 ? "已修改并重新提交" : "只有被驳回的报名才能修改重提");
        return "redirect:/enrollments";
    }
    @PostMapping("/enrollments/{id}/review")
    public String review(@PathVariable Long id, @RequestParam String action,
                         @RequestParam(required = false) BigDecimal requiredAmount,
                         @RequestParam(required = false) BigDecimal plannedHours,
                         @RequestParam(required = false) String reviewNote,
                         HttpSession session, RedirectAttributes ra) {
        SessionUser user = current(session);
        if (!user.hasRole("ACADEMIC")) {
            ra.addFlashAttribute("err", "只有教务可以审核报名");
            return "redirect:/enrollments";
        }
        String status = "approve".equals(action) ? "APPROVED" : "REJECTED";
        if ("approve".equals(action) && (requiredAmount == null || plannedHours == null)) {
            ra.addFlashAttribute("err", "通过审核时必须确认费用和学时");
            return "redirect:/enrollments";
        }
        int rows = enrollmentMapper.review(id, status, requiredAmount, plannedHours,
                user.getUserId(), reviewNote);
        ra.addFlashAttribute(rows > 0 ? "msg" : "err",
                rows > 0 ? "审核完成，结果：" + status : "该报名当前状态不能审核");
        return "redirect:/enrollments";
    }
    // 财务一次登记全额缴费，金额取教务确认的应缴金额
    @PostMapping("/enrollments/{id}/pay")
    public String pay(@PathVariable Long id, @RequestParam String paymentMethod,
                      @RequestParam String paymentVoucher,
                      HttpSession session, RedirectAttributes ra) {
        SessionUser user = current(session);
        if (!user.hasRole("FINANCE")) {
            ra.addFlashAttribute("err", "只有财务可以登记缴费");
            return "redirect:/enrollments";
        }
        Enrollment enrollment = enrollmentMapper.selectById(id);
        if (enrollment == null) {
            ra.addFlashAttribute("err", "报名不存在");
            return "redirect:/enrollments";
        }
        int rows = enrollmentMapper.registerPayment(id, enrollment.getRequiredAmount(),
                paymentMethod, paymentVoucher, user.getUserId());
        ra.addFlashAttribute(rows > 0 ? "msg" : "err",
                rows > 0 ? "缴费登记完成，报名已生效（" + enrollment.getRequiredAmount() + " 元）"
                        : "只有审核通过且未缴费的报名才能登记");
        return "redirect:/enrollments";
    }
}
