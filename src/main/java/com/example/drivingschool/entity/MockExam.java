package com.example.drivingschool.entity;
import lombok.Data;
import java.time.LocalDateTime;
// 对应 mock_exam 表：考试任务，抽题数 20、时长 20 分钟、90 分及格由数据库约束固定
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
