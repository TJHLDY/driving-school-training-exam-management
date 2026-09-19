package com.example.drivingschool.controller;

import com.example.drivingschool.common.ApiResponse;
import com.example.drivingschool.common.PageResult;
import com.example.drivingschool.dto.ExamDtos;
import com.example.drivingschool.service.ExamService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/exams")
public class ExamController {
    private final ExamService examService;

    public ExamController(ExamService examService) {
        this.examService = examService;
    }

    @GetMapping
    public ApiResponse<PageResult<ExamDtos.ExamView>> list(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return ApiResponse.ok(examService.list(status, page, pageSize));
    }

    @GetMapping("/{examId}")
    public ApiResponse<ExamDtos.ExamDetail> get(@PathVariable Long examId) {
        return ApiResponse.ok(examService.get(examId));
    }

    @PostMapping
    @PreAuthorize("hasRole('COACH')")
    public ApiResponse<ExamDtos.ExamView> create(@Valid @RequestBody ExamDtos.ExamSaveRequest request) {
        return ApiResponse.ok(examService.create(request));
    }

    @PutMapping("/{examId}")
    @PreAuthorize("hasRole('COACH')")
    public ApiResponse<ExamDtos.ExamView> update(@PathVariable Long examId,
                                                 @Valid @RequestBody ExamDtos.ExamSaveRequest request) {
        return ApiResponse.ok(examService.update(examId, request));
    }

    @PostMapping("/{examId}/questions")
    @PreAuthorize("hasRole('COACH')")
    public ApiResponse<ExamDtos.QuestionView> addQuestion(
            @PathVariable Long examId, @Valid @RequestBody ExamDtos.QuestionRequest request) {
        return ApiResponse.ok(examService.addQuestion(examId, request));
    }

    @PutMapping("/questions/{questionId}")
    @PreAuthorize("hasRole('COACH')")
    public ApiResponse<ExamDtos.QuestionView> updateQuestion(
            @PathVariable Long questionId, @Valid @RequestBody ExamDtos.QuestionRequest request) {
        return ApiResponse.ok(examService.updateQuestion(questionId, request));
    }

    @DeleteMapping("/questions/{questionId}")
    @PreAuthorize("hasRole('COACH')")
    public ApiResponse<Void> disableQuestion(@PathVariable Long questionId) {
        examService.disableQuestion(questionId);
        return ApiResponse.ok();
    }

    @PostMapping("/{examId}/submit")
    @PreAuthorize("hasRole('COACH')")
    public ApiResponse<ExamDtos.ExamView> submit(@PathVariable Long examId) {
        return ApiResponse.ok(examService.submitForReview(examId));
    }

    @PostMapping("/{examId}/review")
    @PreAuthorize("hasRole('ACADEMIC')")
    public ApiResponse<ExamDtos.ExamView> review(
            @PathVariable Long examId, @Valid @RequestBody ExamDtos.ExamReviewRequest request) {
        return ApiResponse.ok(examService.review(examId, request));
    }

    @PostMapping("/{examId}/close")
    @PreAuthorize("hasRole('ACADEMIC')")
    public ApiResponse<ExamDtos.ExamView> close(@PathVariable Long examId) {
        return ApiResponse.ok(examService.close(examId));
    }

    @PostMapping("/{examId}/attempts")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<ExamDtos.AttemptView> startAttempt(@PathVariable Long examId) {
        return ApiResponse.ok(examService.startAttempt(examId));
    }

    @GetMapping("/attempts/{attemptId}")
    public ApiResponse<ExamDtos.AttemptView> attempt(@PathVariable Long attemptId) {
        return ApiResponse.ok(examService.getAttempt(attemptId));
    }

    @PutMapping("/attempts/{attemptId}/answers/{questionId}")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<ExamDtos.AttemptView> saveAnswer(
            @PathVariable Long attemptId, @PathVariable Long questionId,
            @Valid @RequestBody ExamDtos.SaveAnswerRequest request) {
        return ApiResponse.ok(examService.saveAnswer(attemptId, questionId, request));
    }

    @PostMapping("/attempts/{attemptId}/submit")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<ExamDtos.AttemptView> submitAttempt(@PathVariable Long attemptId) {
        return ApiResponse.ok(examService.submitAttempt(attemptId));
    }

    @GetMapping("/attempts/{attemptId}/result")
    public ApiResponse<ExamDtos.AttemptView> result(@PathVariable Long attemptId) {
        return ApiResponse.ok(examService.getAttempt(attemptId));
    }
}
