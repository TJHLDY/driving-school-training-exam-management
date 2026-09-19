package com.example.drivingschool.mapper;
import com.example.drivingschool.entity.QuestionBank;
import org.apache.ibatis.annotations.Param;
import java.util.List;

// 题库与模拟考试模块：题目映射器，题目归属于考试任务
public interface QuestionBankMapper {
    QuestionBank selectById(@Param("questionId") Long questionId);
    List<QuestionBank> selectList(@Param("examId") Long examId,
                                  @Param("enabled") Boolean enabled,
                                  @Param("keyword") String keyword);
    int countEnabledByExamId(@Param("examId") Long examId);
    int countByExamIdAndStem(@Param("examId") Long examId, @Param("stem") String stem);
    int insert(QuestionBank questionBank);
    int insertBatch(@Param("list") List<QuestionBank> questions);
    int updateById(QuestionBank questionBank);
    int updateEnabled(@Param("questionId") Long questionId, @Param("enabled") Boolean enabled);
    int deleteById(@Param("questionId") Long questionId);
    // 从本考试的启用题中随机抽题，开考时执行一次
    List<QuestionBank> selectRandomByExamId(@Param("examId") Long examId, @Param("limit") Integer limit);
}
