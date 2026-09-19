package com.example.drivingschool.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

public final class ExamDtos {

    private ExamDtos() {
    }

    public record ExamSaveRequest(@NotBlank @Size(max = 80) String examName) {
    }

    public record ExamReviewRequest(@NotNull Boolean approved, @Size(max = 200) String reviewNote) {
    }

    public record QuestionRequest(
            @NotBlank @Size(max = 30) String category,
            @NotBlank @Size(max = 500) String stem,
            @NotBlank @Size(max = 200) String optionA,
            @NotBlank @Size(max = 200) String optionB,
            @NotBlank @Size(max = 200) String optionC,
            @NotBlank @Size(max = 200) String optionD,
            @NotBlank @Pattern(regexp = "^[A-D]$", message = "正确答案必须为A-D") String correctOption,
            @NotBlank @Size(max = 500) String explanation,
            @NotNull Boolean enabled
    ) {
    }

    public record SaveAnswerRequest(@Pattern(regexp = "^$|^[A-D]$", message = "选项必须为A-D") String selectedOption) {
    }

    public record ExamView(String examId, String examName, Integer questionCount, Integer durationMinutes,
                           Integer passScore, String status, String submittedBy, String reviewedBy,
                           LocalDateTime reviewedAt, String reviewNote, LocalDateTime publishedAt) {
    }

    public record QuestionView(String questionId, String examId, String category, String stem, String optionA,
                               String optionB, String optionC, String optionD,
                               String correctOption, String explanation, Boolean enabled) {
    }

    public record ExamDetail(ExamView exam, List<QuestionView> questions) {
    }

    public record AttemptQuestionView(String questionId, Integer positionNo, String category, String stem,
                                      String optionA, String optionB, String optionC, String optionD,
                                      String selectedOption, Integer scoreAwarded, String correctOption,
                                      String explanation) {
    }

    public record AttemptView(String attemptId, String examId, String examName, String studentUserId,
                              String status, LocalDateTime startedAt, LocalDateTime deadlineAt,
                              LocalDateTime submittedAt, Integer score, Boolean passed,
                              List<AttemptQuestionView> questions) {
    }
}
