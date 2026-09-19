package com.example.drivingschool.service;

import com.example.drivingschool.entity.Enrollment;
import com.example.drivingschool.entity.TrainingBooking;
import com.example.drivingschool.entity.UserAccount;
import com.example.drivingschool.exception.BusinessException;
import com.example.drivingschool.mapper.AccountMapper;
import com.example.drivingschool.mapper.EnrollmentMapper;
import com.example.drivingschool.mapper.TrainingMapper;
import com.example.drivingschool.mapper.WithdrawalMapper;
import com.example.drivingschool.security.LoginUser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrainingServiceTest {

    @Mock
    private TrainingMapper trainingMapper;
    @Mock
    private AccountMapper accountMapper;
    @Mock
    private EnrollmentMapper enrollmentMapper;
    @Mock
    private WithdrawalMapper withdrawalMapper;
    @Mock
    private CurrentUserService currentUserService;

    @Test
    void claimRejectsTimeConflictBeforeConditionalUpdate() {
        TrainingService service = new TrainingService(trainingMapper, accountMapper,
                enrollmentMapper, withdrawalMapper, currentUserService);
        LoginUser student = new LoginUser(5L, "student", "hash", Set.of("STUDENT"), true);
        when(currentUserService.requireUser()).thenReturn(student);
        when(accountMapper.findByIdForUpdate(5L)).thenReturn(new UserAccount());
        TrainingBooking booking = new TrainingBooking();
        booking.setBookingId(10L);
        booking.setStatus("OPEN");
        booking.setPlannedStart(LocalDateTime.now().plusDays(1));
        booking.setPlannedEnd(LocalDateTime.now().plusDays(1).plusHours(2));
        when(trainingMapper.findBookingByIdForUpdate(10L)).thenReturn(booking);
        Enrollment enrollment = new Enrollment();
        enrollment.setEnrollmentId(20L);
        enrollment.setStatus("ACTIVE");
        when(enrollmentMapper.findByStudentUserIdForUpdate(5L)).thenReturn(enrollment);
        when(trainingMapper.countOverlaps(anyLong(), any(), any(), any(), any(), anyLong())).thenReturn(1);

        assertThatThrownBy(() -> service.claim(10L)).isInstanceOf(BusinessException.class)
                .hasMessageContaining("已有培训");

        verify(trainingMapper, never()).claim(anyLong(), anyLong(), any());
    }
}
