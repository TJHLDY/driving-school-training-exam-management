package com.example.drivingschool.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ExamAttempt {
    private Long attemptId;
    private Long examId;
    private Long studentUserId;
    private String status;
    private LocalDateTime startedAt;
    private LocalDateTime deadlineAt;
    private LocalDateTime submittedAt;
    private Integer score;
}
