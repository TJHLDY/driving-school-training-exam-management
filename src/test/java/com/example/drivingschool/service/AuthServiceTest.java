package com.example.drivingschool.service;

import com.example.drivingschool.dto.AuthDtos;
import com.example.drivingschool.entity.UserAccount;
import com.example.drivingschool.mapper.AccountMapper;
import com.example.drivingschool.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AccountMapper accountMapper;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private JwtService jwtService;
    @Mock
    private CurrentUserService currentUserService;
    @InjectMocks
    private AuthService authService;

    @Test
    void registerHashesPasswordAndOnlyAssignsStudentRole() {
        AuthDtos.RegisterRequest request = new AuthDtos.RegisterRequest(
                "student01", "Demo@123", "测试学员", "13800000007", "男", null);
        when(accountMapper.findByUsername("student01")).thenReturn(null);
        when(passwordEncoder.encode("Demo@123")).thenReturn("$2a$12$encoded-hash");
        when(accountMapper.findRoleIdByCode("STUDENT")).thenReturn(5L);
        when(jwtService.expiresInSeconds()).thenReturn(7200L);
        when(accountMapper.findMenusByUserId(42L)).thenReturn(List.of());
        doAnswer(invocation -> {
            UserAccount account = invocation.getArgument(0);
            account.setUserId(42L);
            return 1;
        }).when(accountMapper).insertUser(any(UserAccount.class));

        AuthDtos.LoginResponse response = authService.register(request);

        ArgumentCaptor<UserAccount> captor = ArgumentCaptor.forClass(UserAccount.class);
        verify(accountMapper).insertUser(captor.capture());
        assertThat(captor.getValue().getPasswordHash()).isEqualTo("$2a$12$encoded-hash");
        assertThat(captor.getValue().getPasswordHash()).doesNotContain("Demo@123");
        verify(accountMapper).insertUserRole(42L, 5L);
        assertThat(response.roles()).containsExactly("STUDENT");
        assertThat(response.userId()).isEqualTo("42");
    }
}
