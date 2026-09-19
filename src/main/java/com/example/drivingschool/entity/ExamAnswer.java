package com.example.drivingschool.entity;

import lombok.Data;

import java.time.LocalDateTime;

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
