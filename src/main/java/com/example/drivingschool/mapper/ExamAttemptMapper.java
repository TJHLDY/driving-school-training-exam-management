package com.example.drivingschool.mapper;
import com.example.drivingschool.dto.ExamAttemptDetailDto;
import com.example.drivingschool.entity.ExamAttempt;
import org.apache.ibatis.annotations.Param;
import java.util.List;

// 题库与模拟考试模块：答卷映射器
public interface ExamAttemptMapper {
    ExamAttempt selectById(@Param("attemptId") Long attemptId);
    ExamAttemptDetailDto selectDetailById(@Param("attemptId") Long attemptId);
    // 每人每场考试只有一份答卷，查不到才创建
    ExamAttempt selectByExamIdAndStudentUserId(@Param("examId") Long examId,
                                               @Param("studentUserId") Long studentUserId);
    List<ExamAttemptDetailDto> selectDetailListByStudentUserId(@Param("studentUserId") Long studentUserId);
    List<ExamAttemptDetailDto> selectDetailListByExamId(@Param("examId") Long examId);
    int insert(ExamAttempt examAttempt);
    // 交卷判分：只允许 IN_PROGRESS，重复交卷返回 0，成绩不会被覆盖
    int submit(@Param("attemptId") Long attemptId, @Param("score") Integer score);
    // 超时结算，改为 EXPIRED 并保留已保存答案的得分
    int markExpired(@Param("attemptId") Long attemptId, @Param("score") Integer score);
}
