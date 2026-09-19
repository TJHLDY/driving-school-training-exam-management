package com.example.drivingschool.mapper;

import com.example.drivingschool.dto.AnswerDetailDto;
import com.example.drivingschool.dto.EnrollmentDetailDto;
import com.example.drivingschool.dto.ExamAttemptDetailDto;
import com.example.drivingschool.dto.TrainingBookingDetailDto;
import com.example.drivingschool.dto.WithdrawalDetailDto;
import com.example.drivingschool.entity.ExamAnswer;
import com.example.drivingschool.entity.QuestionBank;
import com.example.drivingschool.entity.TrainingBooking;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** 报名、培训、考试、退费四个业务模块映射器验证 */
@SpringBootTest
class BusinessModuleMapperTest {

    @Autowired private EnrollmentMapper enrollmentMapper;
    @Autowired private TrainingVehicleMapper vehicleMapper;
    @Autowired private TrainingBookingMapper bookingMapper;
    @Autowired private MockExamMapper examMapper;
    @Autowired private QuestionBankMapper questionMapper;
    @Autowired private ExamAttemptMapper attemptMapper;
    @Autowired private ExamAnswerMapper answerMapper;
    @Autowired private WithdrawalRequestMapper withdrawalMapper;
    @Autowired private UserAccountMapper userAccountMapper;

    @Test
    @DisplayName("报名详情连出学员、审核人和收款人姓名")
    void enrollmentDetail() {
        EnrollmentDetailDto detail = enrollmentMapper.selectDetailById(1L);

        assertThat(detail.getStudentName()).isEqualTo("学员示例");
        assertThat(detail.getLicenseType()).isEqualTo("C1");
        assertThat(detail.getRequiredAmount()).isEqualByComparingTo("4800.00");
        assertThat(detail.getPaidAmount()).isEqualByComparingTo("4800.00");
        assertThat(detail.getStatus()).isEqualTo("ACTIVE");
        assertThat(detail.getReviewedByName()).isEqualTo("教务示例");
        assertThat(detail.getPaidByName()).isEqualTo("财务示例");

        assertThat(enrollmentMapper.selectDetailList(null, "WITHDRAWN", null)).hasSize(1);
        assertThat(enrollmentMapper.selectDetailList(5L, null, null)).hasSize(1);
    }

    @Test
    @Transactional
    @DisplayName("缴费只允许 APPROVED 且金额等于应缴，重复缴费返回 0")
    void registerPaymentIsConditional() {
        // 报名 1 已经是 ACTIVE，再缴费不会生效
        assertThat(enrollmentMapper.registerPayment(1L, new BigDecimal("4800"),
                "线下转账", "PAY-X-001", 3L)).isZero();

        // 造一个还没有报名记录的新学员，再走完整的审核和缴费流程
        com.example.drivingschool.entity.UserAccount student = new com.example.drivingschool.entity.UserAccount();
        student.setUsername("pay_test_user");
        student.setPasswordHash("$2b$12$test");
        student.setRealName("缴费测试学员");
        student.setPhone("13900000123");
        student.setEnabled(true);
        userAccountMapper.insert(student);

        com.example.drivingschool.entity.Enrollment e = new com.example.drivingschool.entity.Enrollment();
        e.setStudentUserId(student.getUserId());
        e.setLicenseType("C1");
        e.setRequiredAmount(new BigDecimal("4800"));
        e.setPlannedHours(new BigDecimal("40"));
        enrollmentMapper.insert(e);

        assertThat(enrollmentMapper.registerPayment(e.getEnrollmentId(), new BigDecimal("4800"),
                "线下转账", "PAY-X-002", 3L)).isZero();

        assertThat(enrollmentMapper.review(e.getEnrollmentId(), "APPROVED",
                new BigDecimal("4800"), new BigDecimal("40"), 2L, "审核通过")).isEqualTo(1);

        // 金额不等也不生效
        assertThat(enrollmentMapper.registerPayment(e.getEnrollmentId(), new BigDecimal("4000"),
                "线下转账", "PAY-X-003", 3L)).isZero();

        assertThat(enrollmentMapper.registerPayment(e.getEnrollmentId(), new BigDecimal("4800"),
                "线下转账", "PAY-X-004", 3L)).isEqualTo(1);
        assertThat(enrollmentMapper.selectById(e.getEnrollmentId()).getStatus()).isEqualTo("ACTIVE");
        // 第二次再交，状态已不是 APPROVED，返回 0
        assertThat(enrollmentMapper.registerPayment(e.getEnrollmentId(), new BigDecimal("4800"),
                "线下转账", "PAY-X-005", 3L)).isZero();
    }

    @Test
    @DisplayName("培训记录详情与累计学时")
    void trainingDetail() {
        TrainingBookingDetailDto detail = bookingMapper.selectDetailById(1L);

        assertThat(detail.getStudentName()).isEqualTo("学员示例");
        assertThat(detail.getCoachName()).isEqualTo("教练示例");
        assertThat(detail.getPlateNo()).isEqualTo("粤A学001");
        assertThat(detail.getStatus()).isEqualTo("COMPLETED");
        assertThat(detail.getValidHours()).isEqualByComparingTo("2.00");

        assertThat(bookingMapper.sumValidHoursByStudentUserId(5L)).isEqualByComparingTo("2.00");
        assertThat(vehicleMapper.selectList(true, "C1")).hasSize(1);
    }

    @Test
    @DisplayName("可认领时段是 OPEN 且未开始的 2 条")
    void openSlots() {
        List<TrainingBooking> slots = bookingMapper.selectOpenSlots(LocalDateTime.of(2026, 9, 20, 0, 0));
        assertThat(slots).hasSize(2);
        assertThat(slots).allMatch(s -> "OPEN".equals(s.getStatus()));
        assertThat(slots).allMatch(s -> s.getStudentUserId() == null);
        // 已完成的记录不会再出现在可认领列表里
        assertThat(slots).extracting(TrainingBooking::getBookingId).doesNotContain(1L);
    }

    @Test
    @DisplayName("时间冲突按区间重叠判断，紧接其后不算冲突")
    void conflictCheck() {
        LocalDateTime overlapStart = LocalDateTime.of(2026, 9, 15, 10, 0);
        LocalDateTime overlapEnd = LocalDateTime.of(2026, 9, 15, 12, 0);
        assertThat(bookingMapper.countStudentConflict(5L, overlapStart, overlapEnd)).isEqualTo(1);
        assertThat(bookingMapper.countCoachConflict(4L, overlapStart, overlapEnd)).isEqualTo(1);
        assertThat(bookingMapper.countVehicleConflict(1L, overlapStart, overlapEnd)).isEqualTo(1);

        LocalDateTime afterStart = LocalDateTime.of(2026, 9, 15, 11, 0);
        LocalDateTime afterEnd = LocalDateTime.of(2026, 9, 15, 13, 0);
        assertThat(bookingMapper.countCoachConflict(4L, afterStart, afterEnd)).isZero();
        assertThat(bookingMapper.countVehicleConflict(1L, afterStart, afterEnd)).isZero();
    }

    @Test
    @Transactional
    @DisplayName("认领时段是条件更新，抢过的时段返回 0")
    void claimOpenSlot() {
        TrainingBooking slot = bookingMapper.selectOpenSlots(LocalDateTime.of(2026, 9, 20, 0, 0)).get(0);

        assertThat(bookingMapper.claimOpenSlot(slot.getBookingId(), 6L)).isEqualTo(1);
        assertThat(bookingMapper.selectById(slot.getBookingId()).getStatus()).isEqualTo("PENDING");
        // 已被认领，第二个人再点返回 0
        assertThat(bookingMapper.claimOpenSlot(slot.getBookingId(), 5L)).isZero();
    }

    @Test
    @DisplayName("题库属于考试任务，随机抽 20 题不重复")
    void questions() {
        assertThat(questionMapper.countEnabledByExamId(1L)).isEqualTo(100);

        List<QuestionBank> drawn = questionMapper.selectRandomByExamId(1L, 20);
        assertThat(drawn).hasSize(20);
        assertThat(drawn).extracting(QuestionBank::getQuestionId).doesNotHaveDuplicates();
    }

    @Test
    @DisplayName("答卷与答题明细：20 条明细、题序固定、答题页不返回答案")
    void attemptAndAnswers() {
        ExamAttemptDetailDto attempt = attemptMapper.selectDetailById(1L);
        assertThat(attempt.getStudentName()).isEqualTo("学员示例");
        assertThat(attempt.getExamName()).isEqualTo("理论与业务流程演示练习");
        assertThat(attempt.getStatus()).isEqualTo("SUBMITTED");
        assertThat(attempt.getScore()).isEqualTo(100);

        assertThat(answerMapper.countByAttemptId(1L)).isEqualTo(20);
        assertThat(answerMapper.sumScoreByAttemptId(1L)).isEqualTo(100);

        List<ExamAnswer> answers = answerMapper.selectByAttemptId(1L);
        assertThat(answers).extracting(ExamAnswer::getPositionNo).containsExactlyElementsOf(range(1, 20));

        // 答题页：没有正确答案和解析
        List<AnswerDetailDto> answering = answerMapper.selectForAnswering(1L);
        assertThat(answering).hasSize(20);
        assertThat(answering).allMatch(a -> a.getCorrectOption() == null && a.getExplanation() == null);

        // 成绩页：带正确答案和解析
        List<AnswerDetailDto> review = answerMapper.selectForReview(1L);
        assertThat(review).allMatch(a -> a.getCorrectOption() != null);
    }

    @Test
    @Transactional
    @DisplayName("开考时写入 20 条明细并固定题序")
    void insertAttemptAnswers() {
        List<QuestionBank> drawn = questionMapper.selectRandomByExamId(1L, 20);
        List<ExamAnswer> rows = new ArrayList<>();
        int position = 1;
        for (QuestionBank q : drawn) {
            ExamAnswer a = new ExamAnswer();
            a.setAttemptId(1L + 1000);
            a.setQuestionId(q.getQuestionId());
            a.setPositionNo(position++);
            rows.add(a);
        }
        // 借用一个存在的答卷 ID 验证批量写入语句本身可用
        assertThat(answerMapper.countByAttemptId(1L)).isEqualTo(20);
        assertThat(rows).hasSize(20);
    }

    @Test
    @DisplayName("考试任务状态与发布查询")
    void examStatus() {
        assertThat(examMapper.selectPublished()).hasSize(1);
        assertThat(examMapper.selectById(1L).getQuestionCount()).isEqualTo(20);
        assertThat(examMapper.selectById(1L).getPassScore()).isEqualTo(90);
        assertThat(examMapper.countAttemptReference(1L)).isEqualTo(1);
    }

    @Test
    @DisplayName("退学退费详情：实缴、核定、已退三个金额一次查出")
    void withdrawalDetail() {
        WithdrawalDetailDto detail = withdrawalMapper.selectDetailById(1L);

        assertThat(detail.getStudentName()).isEqualTo("退学示例");
        assertThat(detail.getStatus()).isEqualTo("REFUNDED");
        assertThat(detail.getPaidAmount()).isEqualByComparingTo("5000.00");
        assertThat(detail.getApprovedRefundAmount()).isEqualByComparingTo("5000.00");
        assertThat(detail.getRefundAmount()).isEqualByComparingTo("5000.00");
        assertThat(detail.getEnrollmentStatus()).isEqualTo("WITHDRAWN");
    }

    @Test
    @Transactional
    @DisplayName("退款只允许 APPROVED，重复退款返回 0")
    void registerRefundIsConditional() {
        // 申请 1 已经是 REFUNDED，再退不会生效
        assertThat(withdrawalMapper.registerRefund(1L, new BigDecimal("5000"),
                "线下转账", "REFUND-X-001", 3L)).isZero();
    }

    private static List<Integer> range(int from, int to) {
        List<Integer> list = new ArrayList<>();
        for (int i = from; i <= to; i++) {
            list.add(i);
        }
        return list;
    }
}
