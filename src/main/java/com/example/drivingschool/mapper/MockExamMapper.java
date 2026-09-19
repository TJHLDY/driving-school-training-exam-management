package com.example.drivingschool.mapper;
import com.example.drivingschool.entity.MockExam;
import org.apache.ibatis.annotations.Param;
import java.util.List;

// 题库与模拟考试模块：考试任务映射器
public interface MockExamMapper {
    MockExam selectById(@Param("examId") Long examId);
    List<MockExam> selectList(@Param("status") String status);
    List<MockExam> selectBySubmittedBy(@Param("submittedBy") Long submittedBy);
    List<MockExam> selectPublished();
    int insert(MockExam mockExam);
    int updateStatus(@Param("examId") Long examId, @Param("status") String status);
    int review(@Param("examId") Long examId,
               @Param("status") String status,
               @Param("reviewedBy") Long reviewedBy,
               @Param("reviewNote") String reviewNote);
    int publish(@Param("examId") Long examId, @Param("reviewedBy") Long reviewedBy);
    int close(@Param("examId") Long examId);
    int countAttemptReference(@Param("examId") Long examId);
}
