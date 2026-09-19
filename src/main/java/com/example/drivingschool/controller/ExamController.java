package com.example.drivingschool.controller;
import com.example.drivingschool.config.SessionUser;
import com.example.drivingschool.dto.AnswerDetailDto;
import com.example.drivingschool.entity.Enrollment;
import com.example.drivingschool.entity.ExamAnswer;
import com.example.drivingschool.entity.ExamAttempt;
import com.example.drivingschool.entity.MockExam;
import com.example.drivingschool.entity.QuestionBank;
import com.example.drivingschool.mapper.EnrollmentMapper;
import com.example.drivingschool.mapper.ExamAnswerMapper;
import com.example.drivingschool.mapper.ExamAttemptMapper;
import com.example.drivingschool.mapper.MockExamMapper;
import com.example.drivingschool.mapper.QuestionBankMapper;
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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
@Controller

public class ExamController {
    private static final int DRAW_COUNT = 20;
    private static final int SCORE_PER_QUESTION = 5;
    private static final int PASS_SCORE = 90;
    @Autowired private MockExamMapper examMapper;
    @Autowired private QuestionBankMapper questionMapper;
    @Autowired private ExamAttemptMapper attemptMapper;
    @Autowired private ExamAnswerMapper answerMapper;
    @Autowired private EnrollmentMapper enrollmentMapper;
    private SessionUser current(HttpSession session) {
        return (SessionUser) session.getAttribute("loginUser");
    }
    @GetMapping("/exams")
    public String list(HttpSession session, Model model) {
        SessionUser user = current(session);
        model.addAttribute("canManage", user.hasRole("COACH"));
        model.addAttribute("canReview", user.hasRole("ACADEMIC"));
        model.addAttribute("canTake", user.hasRole("STUDENT"));
        model.addAttribute("myExams", user.hasRole("COACH")
                ? examMapper.selectBySubmittedBy(user.getUserId()) : List.of());
        model.addAttribute("allExams", user.hasAnyRole("ACADEMIC", "ADMIN")
                ? examMapper.selectList(null) : List.of());
        model.addAttribute("published", examMapper.selectPublished());
        model.addAttribute("attempts", user.hasRole("STUDENT")
                ? attemptMapper.selectDetailListByStudentUserId(user.getUserId()) : List.of());
        return "exam/list";
    }
    @PostMapping("/exams")
    public String create(@RequestParam String examName, HttpSession session, RedirectAttributes ra) {
        SessionUser user = current(session);
        if (!user.hasRole("COACH")) {
            ra.addFlashAttribute("err", "只有教练可以创建考试任务");
            return "redirect:/exams";
        }
        MockExam exam = new MockExam();
        exam.setExamName(examName);
        exam.setStatus("DRAFT");
        exam.setSubmittedBy(user.getUserId());
        examMapper.insert(exam);
        ra.addFlashAttribute("msg", "考试任务已创建，请添加题目");
        return "redirect:/exams/" + exam.getExamId() + "/questions";
    }
    @GetMapping("/exams/{id}/questions")
    public String questions(@PathVariable Long id, HttpSession session, Model model) {
        SessionUser user = current(session);
        // 题目维护只对教练和管理员开放，其他角色不能通过直接输地址访问
        if (!user.hasAnyRole("COACH", "ADMIN")) {
            return "redirect:/exams";
        }
        MockExam exam = examMapper.selectById(id);
        model.addAttribute("exam", exam);
        model.addAttribute("questions", questionMapper.selectList(id, null, null));
        model.addAttribute("enabledCount", questionMapper.countEnabledByExamId(id));
        // 提交待审核、已发布、已关闭都不允许改题，避免历史成绩对应的题目变化
        model.addAttribute("editable", "DRAFT".equals(exam.getStatus()) || "REJECTED".equals(exam.getStatus()));
        model.addAttribute("canManage", user.hasRole("COACH"));
        return "exam/questions";
    }
    @PostMapping("/exams/{id}/questions")
    public String addQuestion(@PathVariable Long id,
                              @RequestParam String category, @RequestParam String stem,
                              @RequestParam String optionA, @RequestParam String optionB,
                              @RequestParam String optionC, @RequestParam String optionD,
                              @RequestParam String correctOption, @RequestParam String explanation,
                              HttpSession session, RedirectAttributes ra) {
        SessionUser user = current(session);
        MockExam exam = examMapper.selectById(id);
        if (!user.hasRole("COACH")) {
            ra.addFlashAttribute("err", "只有教练可以维护题目");
            return "redirect:/exams/" + id + "/questions";
        }
        if (!"DRAFT".equals(exam.getStatus()) && !"REJECTED".equals(exam.getStatus())) {
            ra.addFlashAttribute("err", "该任务已提交或已发布，不能修改题目");
            return "redirect:/exams/" + id + "/questions";
        }
        if (questionMapper.countByExamIdAndStem(id, stem) > 0) {
            ra.addFlashAttribute("err", "同一考试内题干不能重复");
            return "redirect:/exams/" + id + "/questions";
        }
        QuestionBank q = new QuestionBank();
        q.setExamId(id);
        q.setCategory(category);
        q.setStem(stem);
        q.setOptionA(optionA);
        q.setOptionB(optionB);
        q.setOptionC(optionC);
        q.setOptionD(optionD);
        q.setCorrectOption(correctOption.toUpperCase());
        q.setExplanation(explanation);
        q.setEnabled(true);
        questionMapper.insert(q);
        ra.addFlashAttribute("msg", "题目已添加");
        return "redirect:/exams/" + id + "/questions";
    }
    @PostMapping("/exams/{id}/questions/{questionId}/toggle")
    public String toggleQuestion(@PathVariable Long id, @PathVariable Long questionId,
                                 HttpSession session, RedirectAttributes ra) {
        QuestionBank q = questionMapper.selectById(questionId);
        questionMapper.updateEnabled(questionId, !Boolean.TRUE.equals(q.getEnabled()));
        ra.addFlashAttribute("msg", "题目状态已更新");
        return "redirect:/exams/" + id + "/questions";
    }
    // 教练提交审核：至少 20 道启用题
    @PostMapping("/exams/{id}/submit")
    public String submitExam(@PathVariable Long id, HttpSession session, RedirectAttributes ra) {
        SessionUser user = current(session);
        MockExam exam = examMapper.selectById(id);
        if (!user.hasRole("COACH") || !user.getUserId().equals(exam.getSubmittedBy())) {
            ra.addFlashAttribute("err", "只能提交自己创建的考试任务");
            return "redirect:/exams";
        }
        if (questionMapper.countEnabledByExamId(id) < DRAW_COUNT) {
            ra.addFlashAttribute("err", "启用题目不足 " + DRAW_COUNT + " 道，不能提交审核");
            return "redirect:/exams/" + id + "/questions";
        }
        int rows = examMapper.updateStatus(id, "SUBMITTED");
        ra.addFlashAttribute(rows > 0 ? "msg" : "err", rows > 0 ? "已提交教务审核" : "当前状态不能提交");
        return "redirect:/exams";
    }
    @PostMapping("/exams/{id}/review")
    public String reviewExam(@PathVariable Long id, @RequestParam String action,
                             @RequestParam(required = false) String reviewNote,
                             HttpSession session, RedirectAttributes ra) {
        SessionUser user = current(session);
        if (!user.hasRole("ACADEMIC")) {
            ra.addFlashAttribute("err", "只有教务可以审核考试方案");
            return "redirect:/exams";
        }
        int rows;
        String msg;
        if ("approve".equals(action)) {
            rows = examMapper.publish(id, user.getUserId());
            msg = "考试已发布";
        } else {
            rows = examMapper.review(id, "REJECTED", user.getUserId(), reviewNote);
            msg = "考试已驳回";
        }
        ra.addFlashAttribute(rows > 0 ? "msg" : "err", rows > 0 ? msg : "当前状态不能审核");
        return "redirect:/exams";
    }
    @PostMapping("/exams/{id}/close")
    public String closeExam(@PathVariable Long id, HttpSession session, RedirectAttributes ra) {
        SessionUser user = current(session);
        if (!user.hasRole("ACADEMIC")) {
            ra.addFlashAttribute("err", "只有教务可以关闭考试");
            return "redirect:/exams";
        }
        int rows = examMapper.close(id);
        ra.addFlashAttribute(rows > 0 ? "msg" : "err",
                rows > 0 ? "考试已关闭，不再允许新开答卷" : "只有已发布的考试才能关闭");
        return "redirect:/exams";
    }
    // 开考：同一事务里创建答卷并按随机顺序写入 20 条明细，刷新恢复原卷
    @Transactional
    @PostMapping("/exams/{id}/start")
    public String start(@PathVariable Long id, HttpSession session, RedirectAttributes ra) {
        SessionUser user = current(session);
        if (!user.hasRole("STUDENT")) {
            ra.addFlashAttribute("err", "只有学员可以参加考试");
            return "redirect:/exams";
        }
        Enrollment enrollment = enrollmentMapper.selectByStudentUserId(user.getUserId());
        if (enrollment == null || !"ACTIVE".equals(enrollment.getStatus())) {
            ra.addFlashAttribute("err", "报名生效后才能参加模拟考试");
            return "redirect:/exams";
        }
        MockExam exam = examMapper.selectById(id);
        if (exam == null || !"PUBLISHED".equals(exam.getStatus())) {
            ra.addFlashAttribute("err", "该考试未发布或已关闭");
            return "redirect:/exams";
        }
        ExamAttempt exists = attemptMapper.selectByExamIdAndStudentUserId(id, user.getUserId());
        if (exists != null) {
            return "redirect:/attempts/" + exists.getAttemptId();
        }
        List<QuestionBank> drawn = questionMapper.selectRandomByExamId(id, DRAW_COUNT);
        if (drawn.size() < DRAW_COUNT) {
            ra.addFlashAttribute("err", "题目不足 " + DRAW_COUNT + " 道，无法开考");
            return "redirect:/exams";
        }
        LocalDateTime now = LocalDateTime.now();
        ExamAttempt attempt = new ExamAttempt();
        attempt.setExamId(id);
        attempt.setStudentUserId(user.getUserId());
        attempt.setStatus("IN_PROGRESS");
        attempt.setStartedAt(now);
        attempt.setDeadlineAt(now.plusMinutes(exam.getDurationMinutes()));
        attemptMapper.insert(attempt);
        List<ExamAnswer> rows = new ArrayList<>();
        int position = 1;
        for (QuestionBank q : drawn) {
            ExamAnswer a = new ExamAnswer();
            a.setAttemptId(attempt.getAttemptId());
            a.setQuestionId(q.getQuestionId());
            a.setPositionNo(position++);
            rows.add(a);
        }
        answerMapper.insertBatch(rows);
        return "redirect:/attempts/" + attempt.getAttemptId();
    }
    @GetMapping("/attempts/{id}")
    public String attempt(@PathVariable Long id, HttpSession session, Model model) {
        SessionUser user = current(session);
        ExamAttempt attempt = attemptMapper.selectById(id);
        if (attempt == null || !attempt.getStudentUserId().equals(user.getUserId())) {
            return "redirect:/exams";
        }
        if (!"IN_PROGRESS".equals(attempt.getStatus())) {
            return "redirect:/attempts/" + id + "/result";
        }
        // 超过截止时间就按已保存答案结算
        if (LocalDateTime.now().isAfter(attempt.getDeadlineAt())) {
            int score = answerMapper.sumScoreByAttemptId(id);
            attemptMapper.markExpired(id, score);
            return "redirect:/attempts/" + id + "/result";
        }
        model.addAttribute("attempt", attemptMapper.selectDetailById(id));
        model.addAttribute("answers", answerMapper.selectForAnswering(id));
        return "exam/attempt";
    }
    @PostMapping("/attempts/{id}/save")
    public String saveAnswers(@PathVariable Long id, @RequestParam Map<String, String> params,
                              HttpSession session, RedirectAttributes ra) {
        SessionUser user = current(session);
        ExamAttempt attempt = attemptMapper.selectById(id);
        if (attempt == null || !attempt.getStudentUserId().equals(user.getUserId())
                || !"IN_PROGRESS".equals(attempt.getStatus())) {
            return "redirect:/exams";
        }
        saveSelections(id, params, false);
        ra.addFlashAttribute("msg", "答案已保存，可以稍后继续");
        return "redirect:/attempts/" + id;
    }
    // 交卷：先保存所选，再判分，重复交卷返回原成绩
    @Transactional
    @PostMapping("/attempts/{id}/submit")
    public String submitAttempt(@PathVariable Long id, @RequestParam Map<String, String> params,
                                HttpSession session) {
        SessionUser user = current(session);
        ExamAttempt attempt = attemptMapper.selectById(id);
        if (attempt == null || !attempt.getStudentUserId().equals(user.getUserId())) {
            return "redirect:/exams";
        }
        if (!"IN_PROGRESS".equals(attempt.getStatus())) {
            return "redirect:/attempts/" + id + "/result";
        }
        saveSelections(id, params, true);
        int score = answerMapper.sumScoreByAttemptId(id);
        if (LocalDateTime.now().isAfter(attempt.getDeadlineAt())) {
            attemptMapper.markExpired(id, score);
        } else {
            attemptMapper.submit(id, score);
        }
        return "redirect:/attempts/" + id + "/result";
    }
    // 保存或判分：判分时把每题得分写回明细
    private void saveSelections(Long attemptId, Map<String, String> params, boolean judge) {
        for (ExamAnswer row : answerMapper.selectByAttemptId(attemptId)) {
            String selected = params.get("opt_" + row.getQuestionId());
            if (selected == null || selected.isBlank()) {
                continue;
            }
            Integer score = 0;
            if (judge) {
                QuestionBank q = questionMapper.selectById(row.getQuestionId());
                score = selected.equalsIgnoreCase(q.getCorrectOption()) ? SCORE_PER_QUESTION : 0;
            }
            answerMapper.updateAnswer(attemptId, row.getQuestionId(), selected.toUpperCase(), score);
        }
    }
    @GetMapping("/attempts/{id}/result")
    public String result(@PathVariable Long id, HttpSession session, Model model) {
        SessionUser user = current(session);
        ExamAttempt attempt = attemptMapper.selectById(id);
        if (attempt == null) {
            return "redirect:/exams";
        }
        // 只有本人、教务和管理员可以看成绩
        boolean owner = attempt.getStudentUserId().equals(user.getUserId());
        if (!owner && !user.hasAnyRole("ACADEMIC", "ADMIN")) {
            return "redirect:/exams";
        }
        if ("IN_PROGRESS".equals(attempt.getStatus())) {
            return "redirect:/attempts/" + id;
        }
        Integer score = attempt.getScore() == null ? 0 : attempt.getScore();
        model.addAttribute("attempt", attemptMapper.selectDetailById(id));
        model.addAttribute("score", score);
        model.addAttribute("passed", score >= PASS_SCORE);
        List<AnswerDetailDto> answers = answerMapper.selectForReview(id);
        model.addAttribute("answers", answers);
        model.addAttribute("wrongCount", answers.stream()
                .filter(a -> a.getScoreAwarded() == null || a.getScoreAwarded() == 0).count());
        return "exam/result";
    }
}
