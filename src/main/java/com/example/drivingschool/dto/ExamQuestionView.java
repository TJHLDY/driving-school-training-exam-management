package com.example.drivingschool.dto;

import lombok.Data;

@Data
public class ExamQuestionView {
    private Long attemptId;
    private Long questionId;
    private Integer positionNo;
    private String category;
    private String stem;
    private String optionA;
    private String optionB;
    private String optionC;
    private String optionD;
    private String selectedOption;
    private Integer scoreAwarded;
    private String correctOption;
    private String explanation;
}
