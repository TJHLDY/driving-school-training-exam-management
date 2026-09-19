package com.example.drivingschool.service;

import com.example.drivingschool.dto.ExamDtos;
import com.example.drivingschool.dto.ExamQuestionView;
import com.example.drivingschool.entity.Enrollment;
import com.example.drivingschool.entity.ExamAttempt;
import com.example.drivingschool.entity.MockExam;
import com.example.drivingschool.mapper.AccountMapper;
import com.example.drivingschool.mapper.EnrollmentMapper;
import com.example.drivingschool.mapper.ExamMapper;
import com.example.drivingschool.mapper.WithdrawalMapper;
import com.example.drivingschool.security.LoginUser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExamServiceTest {

    @Mock
    private ExamMapper examMapper;
    @Mock
    private AccountMapper accountMapper;
    @Mock
    private EnrollmentMapper enrollmentMapper;
    @Mock
    private WithdrawalMapper withdrawalMapper;
    @Mock
    private CurrentUserService currentUserService;

    @Test
    void inProgressAttemptDoesNotRevealCorrectAnswerOrExplanation() {
        ExamService service = service();
        LoginUser student = new LoginUser(5L, "student", "hash", Set.of("STUDENT"), true);
        when(currentUserService.requireUser()).thenReturn(student);
        when(enrollmentMapper.findByStudentUserIdForUpdate(5L)).thenReturn(activeEnrollment());
        MockExam exam = publishedExam();
        when(examMapper.findExamByIdForUpdate(1L)).thenReturn(exam);
        ExamAttempt attempt = inProgressAttempt(LocalDateTime.now().plusMinutes(10));
        when(examMapper.findAttemptByExamAndStudent(1L, 5L)).thenReturn(attempt);
        when(examMapper.findAttemptQuestions(1L)).thenReturn(List.of(question()));

        ExamDtos.AttemptView view = service.startAttempt(1L);

        assertThat(view.status()).isEqualTo("IN_PROGRESS");
        assertThat(view.questions()).hasSize(1);
        assertThat(view.questions().get(0).correctOption()).isNull();
        assertThat(view.questions().get(0).explanation()).isNull();
        assertThat(view.questions().get(0).selectedOption()).isEqualTo("A");
    }

    @Test
    void expiredAttemptIsGradedAndThenRevealsAnswers() {
        ExamService service = service();
        LoginUser student = new LoginUser(5L, "student", "hash", Set.of("STUDENT"), true);
        when(currentUserService.requireUser()).thenReturn(student);
        ExamAttempt expired = inProgressAttempt(LocalDateTime.now().minusSeconds(1));
        when(examMapper.findAttemptByIdForUpdate(1L)).thenReturn(expired);
        when(examMapper.gradeAnswers(1L)).thenReturn(20);
        when(examMapper.calculateScore(1L)).thenReturn(95);
        when(examMapper.finishAttempt(org.mockito.ArgumentMatchers.eq(1L),
                org.mockito.ArgumentMatchers.eq("EXPIRED"), org.mockito.ArgumentMatchers.eq(95),
                org.mockito.ArgumentMatchers.any())).thenReturn(1);
        ExamAttempt finished = inProgressAttempt(LocalDateTime.now().minusSeconds(1));
        finished.setStatus("EXPIRED");
        finished.setScore(95);
        finished.setSubmittedAt(LocalDateTime.now());
        when(examMapper.findAttemptById(1L)).thenReturn(finished);
        when(examMapper.findExamById(1L)).thenReturn(publishedExam());
        when(examMapper.findAttemptQuestions(1L)).thenReturn(List.of(question()));

        ExamDtos.AttemptView view = service.getAttempt(1L);

        assertThat(view.status()).isEqualTo("EXPIRED");
        assertThat(view.score()).isEqualTo(95);
        assertThat(view.passed()).isTrue();
        assertThat(view.questions().get(0).correctOption()).isEqualTo("B");
        assertThat(view.questions().get(0).explanation()).isNotBlank();
    }

    private ExamService service() {
        return new ExamService(examMapper, accountMapper, enrollmentMapper, withdrawalMapper, currentUserService);
    }

    private Enrollment activeEnrollment() {
        Enrollment enrollment = new Enrollment();
        enrollment.setEnrollmentId(20L);
        enrollment.setStatus("ACTIVE");
        return enrollment;
    }

    private MockExam publishedExam() {
        MockExam exam = new MockExam();
        exam.setExamId(1L);
        exam.setExamName("模拟考试一");
        exam.setPassScore(90);
        exam.setStatus("PUBLISHED");
        return exam;
    }

    private ExamAttempt inProgressAttempt(LocalDateTime deadline) {
        ExamAttempt attempt = new ExamAttempt();
        attempt.setAttemptId(1L);
        attempt.setExamId(1L);
        attempt.setStudentUserId(5L);
        attempt.setStatus("IN_PROGRESS");
        attempt.setStartedAt(deadline.minusMinutes(20));
        attempt.setDeadlineAt(deadline);
        return attempt;
    }

    private ExamQuestionView question() {
        ExamQuestionView view = new ExamQuestionView();
        view.setAttemptId(1L);
        view.setQuestionId(100L);
        view.setPositionNo(1);
        view.setCategory("安全");
        view.setStem("题目");
        view.setOptionA("A");
        view.setOptionB("B");
        view.setOptionC("C");
        view.setOptionD("D");
        view.setSelectedOption("A");
        view.setScoreAwarded(0);
        view.setCorrectOption("B");
        view.setExplanation("解析");
        return view;
    }
}
