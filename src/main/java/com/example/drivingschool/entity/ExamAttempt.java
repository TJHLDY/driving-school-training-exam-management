package com.example.drivingschool.entity;
import lombok.Data;
import java.time.LocalDateTime;
// 对应 exam_attempt 表：学员答卷，每人每场考试只有一份，由唯一约束保证
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
