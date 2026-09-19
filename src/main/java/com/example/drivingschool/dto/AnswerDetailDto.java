package com.example.drivingschool.dto;
import lombok.Data;
// 答题明细：题目内容连同题序和作答情况，答题时不含正确答案，交卷后才有
@Data

public class AnswerDetailDto {
    private Long answerId;
    private Integer positionNo;
    private Long questionId;
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
