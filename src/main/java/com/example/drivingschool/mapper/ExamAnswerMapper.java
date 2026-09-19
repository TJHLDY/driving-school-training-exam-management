package com.example.drivingschool.mapper;
import com.example.drivingschool.dto.AnswerDetailDto;
import com.example.drivingschool.entity.ExamAnswer;
import org.apache.ibatis.annotations.Param;
import java.util.List;

// 题库与模拟考试模块：答题明细映射器，position_no 固定题序
public interface ExamAnswerMapper {
    List<ExamAnswer> selectByAttemptId(@Param("attemptId") Long attemptId);
    // 答题页用：按题序取题目内容，但不返回正确答案和解析
    List<AnswerDetailDto> selectForAnswering(@Param("attemptId") Long attemptId);
    // 成绩页用：连同正确答案和解析一起返回，只在交卷后调用
    List<AnswerDetailDto> selectForReview(@Param("attemptId") Long attemptId);
    ExamAnswer selectByAttemptIdAndQuestionId(@Param("attemptId") Long attemptId,
                                              @Param("questionId") Long questionId);
    int countByAttemptId(@Param("attemptId") Long attemptId);
    // 开考时按抽题顺序批量写入，题序就是 position_no
    int insertBatch(@Param("list") List<ExamAnswer> answers);
    int updateAnswer(@Param("attemptId") Long attemptId,
                     @Param("questionId") Long questionId,
                     @Param("selectedOption") String selectedOption,
                     @Param("scoreAwarded") Integer scoreAwarded);
    int sumScoreByAttemptId(@Param("attemptId") Long attemptId);
}
