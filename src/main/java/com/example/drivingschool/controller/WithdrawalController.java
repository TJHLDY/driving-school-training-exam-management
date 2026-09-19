package com.example.drivingschool.controller;
import com.example.drivingschool.config.SessionUser;
import com.example.drivingschool.dto.WithdrawalDetailDto;
import com.example.drivingschool.entity.Enrollment;
import com.example.drivingschool.entity.WithdrawalRequest;
import com.example.drivingschool.mapper.EnrollmentMapper;
import com.example.drivingschool.mapper.TrainingBookingMapper;
import com.example.drivingschool.mapper.WithdrawalRequestMapper;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.math.BigDecimal;
@Controller

public class WithdrawalController {
    @Autowired private WithdrawalRequestMapper withdrawalMapper;
    @Autowired private EnrollmentMapper enrollmentMapper;
    @Autowired private TrainingBookingMapper bookingMapper;
    private SessionUser current(HttpSession session) {
        return (SessionUser) session.getAttribute("loginUser");
    }
    @GetMapping("/withdrawals")
    public String list(HttpSession session, Model model) {
        SessionUser user = current(session);
        Long studentUserId = user.isOnlyRole("STUDENT") ? user.getUserId() : null;
        model.addAttribute("withdrawals", withdrawalMapper.selectDetailList(studentUserId, null));
        model.addAttribute("canReview", user.hasRole("ACADEMIC"));
        model.addAttribute("canRefund", user.hasRole("FINANCE"));
        model.addAttribute("canApply", user.hasRole("STUDENT"));
        Enrollment myEnrollment = user.isOnlyRole("STUDENT")
                ? enrollmentMapper.selectByStudentUserId(user.getUserId()) : null;
        model.addAttribute("myEnrollment", myEnrollment);
        return "withdrawal/list";
    }
    // 提交退学申请：只有 ACTIVE 学员可以申请，提交后取消未来预约
    @Transactional
    @PostMapping("/withdrawals")
    public String apply(@RequestParam String reason, HttpSession session, RedirectAttributes ra) {
        SessionUser user = current(session);
        if (!user.hasRole("STUDENT")) {
            ra.addFlashAttribute("err", "只有学员可以提交退学申请");
            return "redirect:/withdrawals";
        }
        Enrollment enrollment = enrollmentMapper.selectByStudentUserId(user.getUserId());
        if (enrollment == null || !"ACTIVE".equals(enrollment.getStatus())) {
            ra.addFlashAttribute("err", "只有报名生效的学员才能申请退学");
            return "redirect:/withdrawals";
        }
        WithdrawalRequest exists = withdrawalMapper.selectByEnrollmentId(enrollment.getEnrollmentId());
        if (exists != null) {
            ra.addFlashAttribute("err", "已存在退学申请，不能重复提交");
            return "redirect:/withdrawals";
        }
        WithdrawalRequest request = new WithdrawalRequest();
        request.setEnrollmentId(enrollment.getEnrollmentId());
        request.setReason(reason);
        request.setStatus("SUBMITTED");
        withdrawalMapper.insert(request);
        bookingMapper.cancelFutureByStudentUserId(user.getUserId());
        ra.addFlashAttribute("msg", "退学申请已提交，未来的预约已取消");
        return "redirect:/withdrawals";
    }
    @PostMapping("/withdrawals/{id}/resubmit")
    public String resubmit(@PathVariable Long id, @RequestParam String reason,
                           HttpSession session, RedirectAttributes ra) {
        SessionUser user = current(session);
        WithdrawalRequest request = withdrawalMapper.selectById(id);
        if (request == null || withdrawalMapper.countProcessingByStudentUserId(user.getUserId()) > 0) {
            ra.addFlashAttribute("err", "当前没有可以修改重提的申请");
            return "redirect:/withdrawals";
        }
        int rows = withdrawalMapper.updateAndResubmit(id, reason);
        ra.addFlashAttribute(rows > 0 ? "msg" : "err",
                rows > 0 ? "已修改并重新提交" : "只有被驳回的申请才能修改重提");
        return "redirect:/withdrawals";
    }
    // 教务核定：核定金额不能超过报名实缴金额
    @PostMapping("/withdrawals/{id}/review")
    public String review(@PathVariable Long id, @RequestParam String action,
                         @RequestParam(required = false) BigDecimal approvedRefundAmount,
                         @RequestParam(required = false) String reviewNote,
                         HttpSession session, RedirectAttributes ra) {
        SessionUser user = current(session);
        if (!user.hasRole("ACADEMIC")) {
            ra.addFlashAttribute("err", "只有教务可以核定退费");
            return "redirect:/withdrawals";
        }
        WithdrawalDetailDto detail = withdrawalMapper.selectDetailById(id);
        if (detail == null || !"SUBMITTED".equals(detail.getStatus())) {
            ra.addFlashAttribute("err", "该申请当前状态不能核定");
            return "redirect:/withdrawals";
        }
        if ("approve".equals(action)) {
            if (approvedRefundAmount == null || approvedRefundAmount.compareTo(BigDecimal.ZERO) < 0) {
                ra.addFlashAttribute("err", "核定金额不能为空且不能为负");
                return "redirect:/withdrawals";
            }
            if (approvedRefundAmount.compareTo(detail.getPaidAmount()) > 0) {
                ra.addFlashAttribute("err", "核定金额不能超过实缴金额 " + detail.getPaidAmount() + " 元");
                return "redirect:/withdrawals";
            }
        }
        String status = "approve".equals(action) ? "APPROVED" : "REJECTED";
        int rows = withdrawalMapper.review(id, status, user.getUserId(), reviewNote,
                "approve".equals(action) ? approvedRefundAmount : null);
        ra.addFlashAttribute(rows > 0 ? "msg" : "err",
                rows > 0 ? "核定完成，结果：" + status : "核定失败，请刷新重试");
        return "redirect:/withdrawals";
    }
    // 财务登记退款并结算：退款额等于核定额，同时把报名改为 WITHDRAWN
    @Transactional
    @PostMapping("/withdrawals/{id}/refund")
    public String refund(@PathVariable Long id, @RequestParam String refundMethod,
                         @RequestParam String refundVoucher,
                         HttpSession session, RedirectAttributes ra) {
        SessionUser user = current(session);
        if (!user.hasRole("FINANCE")) {
            ra.addFlashAttribute("err", "只有财务可以登记退款");
            return "redirect:/withdrawals";
        }
        WithdrawalDetailDto detail = withdrawalMapper.selectDetailById(id);
        if (detail == null || !"APPROVED".equals(detail.getStatus())) {
            ra.addFlashAttribute("err", "只有已核定的申请才能登记退款");
            return "redirect:/withdrawals";
        }
        BigDecimal amount = detail.getApprovedRefundAmount();
        int rows = withdrawalMapper.registerRefund(id, amount, refundMethod, refundVoucher, user.getUserId());
        if (rows > 0) {
            enrollmentMapper.markWithdrawn(detail.getEnrollmentId());
            ra.addFlashAttribute("msg", "退款登记完成，已退款 " + amount + " 元，报名状态改为 WITHDRAWN");
        } else {
            ra.addFlashAttribute("err", "退款登记失败，可能已经结算过");
        }
        return "redirect:/withdrawals";
    }
}
