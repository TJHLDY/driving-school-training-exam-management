package com.example.drivingschool.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MockExam {
    private Long examId;
    private String examName;
    private Integer questionCount;
    private Integer durationMinutes;
    private Integer passScore;
    private String status;
    private Long submittedBy;
    private Long reviewedBy;
    private LocalDateTime reviewedAt;
    private String reviewNote;
    private LocalDateTime publishedAt;
}
