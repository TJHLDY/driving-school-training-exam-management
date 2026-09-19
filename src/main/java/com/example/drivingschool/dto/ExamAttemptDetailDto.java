package com.example.drivingschool.dto;
import lombok.Data;
import java.time.LocalDateTime;
// 答卷详情：答卷连同考试名称和学员姓名
@Data

public class ExamAttemptDetailDto {
    private Long attemptId;
    private Long examId;
    private String examName;
    private Long studentUserId;
    private String studentName;
    private String status;
    private LocalDateTime startedAt;
    private LocalDateTime deadlineAt;
    private LocalDateTime submittedAt;
    private Integer score;
}
