package com.example.drivingschool.mapper;

import com.example.drivingschool.dto.ExamQuestionView;
import com.example.drivingschool.entity.ExamAnswer;
import com.example.drivingschool.entity.ExamAttempt;
import com.example.drivingschool.entity.MockExam;
import com.example.drivingschool.entity.QuestionBank;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ExamMapper {
    MockExam findExamById(@Param("examId") Long examId);
    MockExam findExamByIdForUpdate(@Param("examId") Long examId);
    int insertExam(MockExam exam);
    int updateExam(MockExam exam);
    int submitExam(@Param("examId") Long examId);
    int reviewExam(@Param("examId") Long examId, @Param("status") String status,
                   @Param("reviewedBy") Long reviewedBy, @Param("reviewedAt") LocalDateTime reviewedAt,
                   @Param("reviewNote") String reviewNote, @Param("publishedAt") LocalDateTime publishedAt);
    int closeExam(@Param("examId") Long examId);
    List<MockExam> findExamPage(@Param("submittedBy") Long submittedBy, @Param("status") String status,
                                @Param("offset") long offset, @Param("size") int size);
    long countExams(@Param("submittedBy") Long submittedBy, @Param("status") String status);
    List<MockExam> findPublishedExamPage(@Param("offset") long offset, @Param("size") int size);
    long countPublishedExams();

    QuestionBank findQuestionById(@Param("questionId") Long questionId);
    int insertQuestion(QuestionBank question);
    int updateQuestion(QuestionBank question);
    int disableQuestion(@Param("questionId") Long questionId);
    int countEnabledQuestions(@Param("examId") Long examId);
    List<QuestionBank> findEnabledQuestions(@Param("examId") Long examId);
    List<QuestionBank> findRandomQuestions(@Param("examId") Long examId);

    ExamAttempt findAttemptById(@Param("attemptId") Long attemptId);
    ExamAttempt findAttemptByIdForUpdate(@Param("attemptId") Long attemptId);
    ExamAttempt findAttemptByExamAndStudent(@Param("examId") Long examId, @Param("studentUserId") Long studentUserId);
    int insertAttempt(ExamAttempt attempt);
    int insertAnswer(@Param("attemptId") Long attemptId, @Param("questionId") Long questionId,
                     @Param("positionNo") int positionNo);
    ExamAnswer findAnswer(@Param("attemptId") Long attemptId, @Param("questionId") Long questionId);
    int saveAnswer(@Param("attemptId") Long attemptId, @Param("questionId") Long questionId,
                   @Param("selectedOption") String selectedOption, @Param("answeredAt") LocalDateTime answeredAt);
    int gradeAnswers(@Param("attemptId") Long attemptId);
    int calculateScore(@Param("attemptId") Long attemptId);
    int finishAttempt(@Param("attemptId") Long attemptId, @Param("status") String status,
                      @Param("score") int score, @Param("submittedAt") LocalDateTime submittedAt);
    List<ExamQuestionView> findAttemptQuestions(@Param("attemptId") Long attemptId);
}
