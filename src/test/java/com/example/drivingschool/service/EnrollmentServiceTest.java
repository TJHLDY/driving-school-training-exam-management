package com.example.drivingschool.service;

import com.example.drivingschool.config.EnrollmentProperties;
import com.example.drivingschool.dto.EnrollmentDtos;
import com.example.drivingschool.entity.Enrollment;
import com.example.drivingschool.mapper.EnrollmentMapper;
import com.example.drivingschool.security.LoginUser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnrollmentServiceTest {

    @Mock
    private EnrollmentMapper enrollmentMapper;
    @Mock
    private CurrentUserService currentUserService;

    @Test
    void studentCannotChoosePriceAndBackendUsesConfirmedDefault() {
        EnrollmentProperties properties = new EnrollmentProperties(
                new BigDecimal("4800.00"), new BigDecimal("5000.00"), new BigDecimal("40.00"));
        EnrollmentService service = new EnrollmentService(enrollmentMapper, currentUserService, properties);
        LoginUser student = new LoginUser(5L, "student", "hash", Set.of("STUDENT"), true);
        when(currentUserService.requireUser()).thenReturn(student);
        when(enrollmentMapper.findByStudentUserIdForUpdate(5L)).thenReturn(null);
        doAnswer(invocation -> {
            Enrollment enrollment = invocation.getArgument(0);
            enrollment.setEnrollmentId(10L);
            return 1;
        }).when(enrollmentMapper).insert(any(Enrollment.class));

        Enrollment persisted = new Enrollment();
        persisted.setEnrollmentId(10L);
        persisted.setStudentUserId(5L);
        persisted.setLicenseType("C1");
        persisted.setRequiredAmount(new BigDecimal("4800.00"));
        persisted.setPlannedHours(new BigDecimal("40.00"));
        persisted.setStatus("SUBMITTED");
        when(enrollmentMapper.findById(10L)).thenReturn(persisted);

        EnrollmentDtos.EnrollmentView response = service.submit(new EnrollmentDtos.SubmitRequest("C1"));

        ArgumentCaptor<Enrollment> captor = ArgumentCaptor.forClass(Enrollment.class);
        verify(enrollmentMapper).insert(captor.capture());
        assertThat(captor.getValue().getRequiredAmount()).isEqualByComparingTo("4800.00");
        assertThat(captor.getValue().getPlannedHours()).isEqualByComparingTo("40.00");
        assertThat(response.status()).isEqualTo("SUBMITTED");
    }
}
