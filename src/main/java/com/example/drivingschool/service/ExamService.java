package com.example.drivingschool.service;

import com.example.drivingschool.common.PageResult;
import com.example.drivingschool.common.PageUtils;
import com.example.drivingschool.dto.ExamDtos;
import com.example.drivingschool.dto.ExamQuestionView;
import com.example.drivingschool.entity.Enrollment;
import com.example.drivingschool.entity.ExamAttempt;
import com.example.drivingschool.entity.MockExam;
import com.example.drivingschool.entity.QuestionBank;
import com.example.drivingschool.exception.BusinessException;
import com.example.drivingschool.mapper.AccountMapper;
import com.example.drivingschool.mapper.EnrollmentMapper;
import com.example.drivingschool.mapper.ExamMapper;
import com.example.drivingschool.mapper.WithdrawalMapper;
import com.example.drivingschool.security.LoginUser;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
public class ExamService {
    private final ExamMapper examMapper;
    private final AccountMapper accountMapper;
    private final EnrollmentMapper enrollmentMapper;
    private final WithdrawalMapper withdrawalMapper;
    private final CurrentUserService currentUserService;

    public ExamService(ExamMapper examMapper, AccountMapper accountMapper, EnrollmentMapper enrollmentMapper,
                       WithdrawalMapper withdrawalMapper, CurrentUserService currentUserService) {
        this.examMapper = examMapper;
        this.accountMapper = accountMapper;
        this.enrollmentMapper = enrollmentMapper;
        this.withdrawalMapper = withdrawalMapper;
        this.currentUserService = currentUserService;
    }

    public PageResult<ExamDtos.ExamView> list(String status, Integer pageNo, Integer pageSize) {
        LoginUser user = currentUserService.requireUser();
        int page = PageUtils.page(pageNo);
        int size = PageUtils.pageSize(pageSize);
        long offset = PageUtils.offset(page, size);
        List<MockExam> exams;
        long total;
        if (user.roleCodes().contains("STUDENT")) {
            if (status != null && !List.of("PUBLISHED", "CLOSED").contains(status)) {
                throw new BusinessException(HttpStatus.FORBIDDEN, "学员只能查看已发布考试");
            }
            exams = examMapper.findPublishedExamPage(offset, size);
            total = examMapper.countPublishedExams();
        } else if (user.roleCodes().contains("COACH") && !user.roleCodes().contains("ACADEMIC")
                && !user.roleCodes().contains("ADMIN")) {
            exams = examMapper.findExamPage(user.userId(), status, offset, size);
            total = examMapper.countExams(user.userId(), status);
        } else if (user.roleCodes().stream().anyMatch(List.of("ACADEMIC", "ADMIN")::contains)) {
            exams = examMapper.findExamPage(null, status, offset, size);
            total = examMapper.countExams(null, status);
        } else {
            throw new BusinessException(HttpStatus.FORBIDDEN, "无权查询考试");
        }
        return new PageResult<>(exams.stream().map(this::toExamView).toList(), total, page, size);
    }

    public ExamDtos.ExamDetail get(Long examId) {
        MockExam exam = requireExam(examId);
        LoginUser user = currentUserService.requireUser();
        boolean staff = user.roleCodes().stream().anyMatch(List.of("ACADEMIC", "ADMIN")::contains);
        boolean owner = user.userId().equals(exam.getSubmittedBy()) && user.roleCodes().contains("COACH");
        boolean studentVisible = user.roleCodes().contains("STUDENT")
                && List.of("PUBLISHED", "CLOSED").contains(exam.getStatus());
        if (!staff && !owner && !studentVisible) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "无权查看该考试");
        }
        List<ExamDtos.QuestionView> questions = (staff || owner)
                ? examMapper.findEnabledQuestions(examId).stream().map(this::toQuestionView).toList()
                : List.of();
        return new ExamDtos.ExamDetail(toExamView(exam), questions);
    }

    @Transactional
    public ExamDtos.ExamView create(ExamDtos.ExamSaveRequest request) {
        Long actorId = currentUserService.userId();
        currentUserService.requireAnyRole("COACH");
        MockExam exam = new MockExam();
        exam.setExamName(request.examName().trim());
        exam.setSubmittedBy(actorId);
        examMapper.insertExam(exam);
        return toExamView(examMapper.findExamById(exam.getExamId()));
    }

    @Transactional
    public ExamDtos.ExamView update(Long examId, ExamDtos.ExamSaveRequest request) {
        LoginUser user = currentUserService.requireUser();
        currentUserService.requireAnyRole("COACH");
        MockExam exam = requireExamForUpdate(examId);
        requireEditableByCoach(exam, user.userId());
        exam.setExamName(request.examName().trim());
        if (examMapper.updateExam(exam) != 1) {
            throw new BusinessException(HttpStatus.CONFLICT, "当前考试状态不允许修改");
        }
        return toExamView(examMapper.findExamById(examId));
    }

    @Transactional
    public ExamDtos.QuestionView addQuestion(Long examId, ExamDtos.QuestionRequest request) {
        LoginUser user = currentUserService.requireUser();
        currentUserService.requireAnyRole("COACH");
        MockExam exam = requireExamForUpdate(examId);
        requireEditableByCoach(exam, user.userId());
        QuestionBank question = fromQuestionRequest(examId, request);
        examMapper.insertQuestion(question);
        return toQuestionView(question);
    }

    @Transactional
    public ExamDtos.QuestionView updateQuestion(Long questionId, ExamDtos.QuestionRequest request) {
        LoginUser user = currentUserService.requireUser();
        currentUserService.requireAnyRole("COACH");
        QuestionBank question = examMapper.findQuestionById(questionId);
        if (question == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "题目不存在");
        }
        MockExam exam = requireExamForUpdate(question.getExamId());
        requireEditableByCoach(exam, user.userId());
        QuestionBank replacement = fromQuestionRequest(exam.getExamId(), request);
        replacement.setQuestionId(questionId);
        examMapper.updateQuestion(replacement);
        return toQuestionView(replacement);
    }

    @Transactional
    public void disableQuestion(Long questionId) {
        LoginUser user = currentUserService.requireUser();
        currentUserService.requireAnyRole("COACH");
        QuestionBank question = examMapper.findQuestionById(questionId);
        if (question == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "题目不存在");
        }
        MockExam exam = requireExamForUpdate(question.getExamId());
        requireEditableByCoach(exam, user.userId());
        examMapper.disableQuestion(questionId);
    }

    @Transactional
    public ExamDtos.ExamView submitForReview(Long examId) {
        LoginUser user = currentUserService.requireUser();
        currentUserService.requireAnyRole("COACH");
        MockExam exam = requireExamForUpdate(examId);
        requireEditableByCoach(exam, user.userId());
        if (examMapper.countEnabledQuestions(examId) < 20) {
            throw new BusinessException(HttpStatus.CONFLICT, "至少需要20道启用题目");
        }
        if (examMapper.submitExam(examId) != 1) {
            throw new BusinessException(HttpStatus.CONFLICT, "考试状态已变化，请刷新后重试");
        }
        return toExamView(examMapper.findExamById(examId));
    }

    @Transactional
    public ExamDtos.ExamView review(Long examId, ExamDtos.ExamReviewRequest request) {
        currentUserService.requireAnyRole("ACADEMIC");
        MockExam exam = requireExamForUpdate(examId);
        if (!"SUBMITTED".equals(exam.getStatus())) {
            throw new BusinessException(HttpStatus.CONFLICT, "只有待审核考试可以审核");
        }
        String status = request.approved() ? "PUBLISHED" : "REJECTED";
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime publishedAt = request.approved() ? now : null;
        if (examMapper.reviewExam(examId, status, currentUserService.userId(), now,
                request.reviewNote(), publishedAt) != 1) {
            throw new BusinessException(HttpStatus.CONFLICT, "考试状态已变化，请刷新后重试");
        }
        return toExamView(examMapper.findExamById(examId));
    }

    @Transactional
    public ExamDtos.ExamView close(Long examId) {
        currentUserService.requireAnyRole("ACADEMIC");
        MockExam exam = requireExamForUpdate(examId);
        if (!"PUBLISHED".equals(exam.getStatus())) {
            throw new BusinessException(HttpStatus.CONFLICT, "只有已发布考试可以关闭");
        }
        examMapper.closeExam(examId);
        return toExamView(examMapper.findExamById(examId));
    }

    @Transactional
    public ExamDtos.AttemptView startAttempt(Long examId) {
        LoginUser user = currentUserService.requireUser();
        currentUserService.requireAnyRole("STUDENT");
        accountMapper.findByIdForUpdate(user.userId());
        requireActiveStudent(user.userId());
        MockExam exam = requireExamForUpdate(examId);
        if (!"PUBLISHED".equals(exam.getStatus())) {
            throw new BusinessException(HttpStatus.CONFLICT, "考试尚未发布或已关闭");
        }
        ExamAttempt existing = examMapper.findAttemptByExamAndStudent(examId, user.userId());
        if (existing != null) {
            if (isExpired(existing)) {
                return finishAttempt(existing, true);
            }
            return toAttemptView(existing, exam);
        }
        if (examMapper.countEnabledQuestions(examId) < 20) {
            throw new BusinessException(HttpStatus.CONFLICT, "考试题目不足20道");
        }
        LocalDateTime now = LocalDateTime.now();
        ExamAttempt attempt = new ExamAttempt();
        attempt.setExamId(examId);
        attempt.setStudentUserId(user.userId());
        attempt.setStartedAt(now);
        attempt.setDeadlineAt(now.plusMinutes(20));
        try {
            examMapper.insertAttempt(attempt);
        } catch (DataIntegrityViolationException ex) {
            ExamAttempt concurrent = examMapper.findAttemptByExamAndStudent(examId, user.userId());
            if (concurrent == null) {
                throw ex;
            }
            return toAttemptView(concurrent, exam);
        }
        List<QuestionBank> questions = examMapper.findRandomQuestions(examId);
        if (questions.size() != 20) {
            throw new BusinessException(HttpStatus.CONFLICT, "随机组卷失败，题目数量不足");
        }
        for (int i = 0; i < questions.size(); i++) {
            examMapper.insertAnswer(attempt.getAttemptId(), questions.get(i).getQuestionId(), i + 1);
        }
        return toAttemptView(examMapper.findAttemptById(attempt.getAttemptId()), exam);
    }

    @Transactional
    public ExamDtos.AttemptView saveAnswer(Long attemptId, Long questionId, ExamDtos.SaveAnswerRequest request) {
        LoginUser user = currentUserService.requireUser();
        currentUserService.requireAnyRole("STUDENT");
        ExamAttempt attempt = requireAttemptForUpdate(attemptId);
        if (!user.userId().equals(attempt.getStudentUserId())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "不能修改他人答卷");
        }
        if (!"IN_PROGRESS".equals(attempt.getStatus())) {
            return toAttemptView(attempt, requireExam(attempt.getExamId()));
        }
        if (isExpired(attempt)) {
            return finishAttempt(attempt, true);
        }
        if (examMapper.findAnswer(attemptId, questionId) == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "题目不属于该答卷");
        }
        String selected = request.selectedOption();
        selected = selected == null || selected.isBlank() ? null : selected.trim().toUpperCase(Locale.ROOT);
        examMapper.saveAnswer(attemptId, questionId, selected, LocalDateTime.now());
        return toAttemptView(examMapper.findAttemptById(attemptId), requireExam(attempt.getExamId()));
    }

    @Transactional
    public ExamDtos.AttemptView submitAttempt(Long attemptId) {
        LoginUser user = currentUserService.requireUser();
        currentUserService.requireAnyRole("STUDENT");
        ExamAttempt attempt = requireAttemptForUpdate(attemptId);
        if (!user.userId().equals(attempt.getStudentUserId())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "不能提交他人答卷");
        }
        if (!"IN_PROGRESS".equals(attempt.getStatus())) {
            return toAttemptView(attempt, requireExam(attempt.getExamId()));
        }
        return finishAttempt(attempt, isExpired(attempt));
    }

    @Transactional
    public ExamDtos.AttemptView getAttempt(Long attemptId) {
        LoginUser user = currentUserService.requireUser();
        ExamAttempt attempt = requireAttemptForUpdate(attemptId);
        boolean owner = user.roleCodes().contains("STUDENT") && user.userId().equals(attempt.getStudentUserId());
        boolean staff = user.roleCodes().stream().anyMatch(List.of("ACADEMIC", "ADMIN")::contains);
        if (!owner && !staff) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "无权查看该答卷");
        }
        if ("IN_PROGRESS".equals(attempt.getStatus()) && isExpired(attempt)) {
            return finishAttempt(attempt, true);
        }
        return toAttemptView(attempt, requireExam(attempt.getExamId()));
    }

    private ExamDtos.AttemptView finishAttempt(ExamAttempt attempt, boolean expired) {
        examMapper.gradeAnswers(attempt.getAttemptId());
        int score = examMapper.calculateScore(attempt.getAttemptId());
        String status = expired ? "EXPIRED" : "SUBMITTED";
        if (examMapper.finishAttempt(attempt.getAttemptId(), status, score, LocalDateTime.now()) == 1) {
            attempt = examMapper.findAttemptById(attempt.getAttemptId());
        }
        return toAttemptView(attempt, requireExam(attempt.getExamId()));
    }

    private ExamDtos.AttemptView toAttemptView(ExamAttempt attempt, MockExam exam) {
        boolean finished = !"IN_PROGRESS".equals(attempt.getStatus());
        List<ExamDtos.AttemptQuestionView> questions = examMapper.findAttemptQuestions(attempt.getAttemptId()).stream()
                .map(view -> toAttemptQuestionView(view, finished)).toList();
        Boolean passed = attempt.getScore() == null ? null : attempt.getScore() >= exam.getPassScore();
        return new ExamDtos.AttemptView(String.valueOf(attempt.getAttemptId()), String.valueOf(attempt.getExamId()),
                exam.getExamName(), String.valueOf(attempt.getStudentUserId()), attempt.getStatus(),
                attempt.getStartedAt(), attempt.getDeadlineAt(), attempt.getSubmittedAt(), attempt.getScore(),
                passed, questions);
    }

    private ExamDtos.AttemptQuestionView toAttemptQuestionView(ExamQuestionView q, boolean revealAnswer) {
        return new ExamDtos.AttemptQuestionView(String.valueOf(q.getQuestionId()), q.getPositionNo(), q.getCategory(),
                q.getStem(), q.getOptionA(), q.getOptionB(), q.getOptionC(), q.getOptionD(), q.getSelectedOption(),
                revealAnswer ? q.getScoreAwarded() : null, revealAnswer ? q.getCorrectOption() : null,
                revealAnswer ? q.getExplanation() : null);
    }

    private boolean isExpired(ExamAttempt attempt) {
        return "IN_PROGRESS".equals(attempt.getStatus()) && !LocalDateTime.now().isBefore(attempt.getDeadlineAt());
    }

    private void requireActiveStudent(Long userId) {
        Enrollment enrollment = enrollmentMapper.findByStudentUserIdForUpdate(userId);
        if (enrollment == null || !"ACTIVE".equals(enrollment.getStatus())) {
            throw new BusinessException(HttpStatus.CONFLICT, "只有缴费生效学员可以参加考试");
        }
        var withdrawal = withdrawalMapper.findByEnrollmentId(enrollment.getEnrollmentId());
        if (withdrawal != null && List.of("SUBMITTED", "APPROVED").contains(withdrawal.getStatus())) {
            throw new BusinessException(HttpStatus.CONFLICT, "退学处理中，不能参加考试");
        }
    }

    private MockExam requireExam(Long examId) {
        MockExam exam = examMapper.findExamById(examId);
        if (exam == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "考试不存在");
        }
        return exam;
    }

    private MockExam requireExamForUpdate(Long examId) {
        MockExam exam = examMapper.findExamByIdForUpdate(examId);
        if (exam == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "考试不存在");
        }
        return exam;
    }

    private ExamAttempt requireAttemptForUpdate(Long attemptId) {
        ExamAttempt attempt = examMapper.findAttemptByIdForUpdate(attemptId);
        if (attempt == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "答卷不存在");
        }
        return attempt;
    }

    private void requireEditableByCoach(MockExam exam, Long actorId) {
        if (!actorId.equals(exam.getSubmittedBy())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "只能维护自己创建的考试");
        }
        if (!List.of("DRAFT", "REJECTED").contains(exam.getStatus())) {
            throw new BusinessException(HttpStatus.CONFLICT, "已提交、已发布或已关闭考试不能修改题目");
        }
    }

    private QuestionBank fromQuestionRequest(Long examId, ExamDtos.QuestionRequest request) {
        QuestionBank question = new QuestionBank();
        question.setExamId(examId);
        question.setCategory(request.category().trim());
        question.setStem(request.stem().trim());
        question.setOptionA(request.optionA().trim());
        question.setOptionB(request.optionB().trim());
        question.setOptionC(request.optionC().trim());
        question.setOptionD(request.optionD().trim());
        question.setCorrectOption(request.correctOption().trim().toUpperCase(Locale.ROOT));
        question.setExplanation(request.explanation().trim());
        question.setEnabled(request.enabled());
        return question;
    }

    private ExamDtos.ExamView toExamView(MockExam e) {
        return new ExamDtos.ExamView(String.valueOf(e.getExamId()), e.getExamName(), e.getQuestionCount(),
                e.getDurationMinutes(), e.getPassScore(), e.getStatus(), stringId(e.getSubmittedBy()),
                stringId(e.getReviewedBy()), e.getReviewedAt(), e.getReviewNote(), e.getPublishedAt());
    }

    private ExamDtos.QuestionView toQuestionView(QuestionBank q) {
        return new ExamDtos.QuestionView(String.valueOf(q.getQuestionId()), String.valueOf(q.getExamId()),
                q.getCategory(), q.getStem(), q.getOptionA(), q.getOptionB(), q.getOptionC(), q.getOptionD(),
                q.getCorrectOption(), q.getExplanation(), q.getEnabled());
    }

    private String stringId(Long id) {
        return id == null ? null : String.valueOf(id);
    }
}
