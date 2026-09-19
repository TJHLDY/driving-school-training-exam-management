package com.example.drivingschool.entity;

import lombok.Data;

@Data
public class QuestionBank {
    private Long questionId;
    private Long examId;
    private String category;
    private String stem;
    private String optionA;
    private String optionB;
    private String optionC;
    private String optionD;
    private String correctOption;
    private String explanation;
    private Boolean enabled;
}
