package com.example.drivingschool.entity;
import lombok.Data;
import java.time.LocalDateTime;
// 对应 exam_answer 表：答题明细，position_no 就是答卷内的题序，抽题后一次写入固定不变
@Data

public class ExamAnswer {
    private Long answerId;
    private Long attemptId;
    private Long questionId;
    private Integer positionNo;
    private String selectedOption;
    private Integer scoreAwarded;
    private LocalDateTime answeredAt;
}
