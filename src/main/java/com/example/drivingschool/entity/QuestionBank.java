package com.example.drivingschool.entity;
import lombok.Data;
// 对应 question_bank 表：题目直接归属于某个考试任务，不设公共题库到试卷的中间表
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
