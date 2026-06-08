package com.example.food.auth;

import com.example.food.admin.AdminMapper;
import com.example.food.auth.dto.AuthResponse;
import com.example.food.auth.dto.PhoneLoginRequest;
import com.example.food.security.AuthPrincipal;
import com.example.food.security.JwtService;
import com.example.food.user.User;
import com.example.food.user.UserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private AdminMapper adminMapper;

    @Mock
    private JwtService jwtService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Test
    void loginUserUsesReadCommittedTransactionIsolation() throws NoSuchMethodException {
        Method loginUser = AuthService.class.getMethod("loginUser", PhoneLoginRequest.class);

        Transactional transactional = loginUser.getAnnotation(Transactional.class);

        assertThat(transactional).isNotNull();
        assertThat(transactional.isolation()).isEqualTo(Isolation.READ_COMMITTED);
    }

    @Test
    void loginUserRecoversWhenConcurrentInsertCreatesSamePhone() {
        AuthService authService = new AuthService(userMapper, adminMapper, jwtService, passwordEncoder, "123456");
        User concurrentUser = new User();
        concurrentUser.setId(42L);
        concurrentUser.setPhone("13900009999");
        concurrentUser.setNickname("13900009999");
        concurrentUser.setRole("USER");
        concurrentUser.setEnabled(true);

        when(userMapper.selectOne(any())).thenReturn(null, concurrentUser);
        when(userMapper.insert(any(User.class))).thenThrow(new DuplicateKeyException("duplicate phone"));
        when(jwtService.generateToken(any(AuthPrincipal.class))).thenReturn("jwt-token");

        AuthResponse response = authService.loginUser(new PhoneLoginRequest("13900009999", "123456"));

        assertThat(response.id()).isEqualTo(42L);
        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.role()).isEqualTo("USER");
        assertThat(concurrentUser.getLastLoginAt()).isNotNull();
        verify(userMapper, times(2)).selectOne(any());
        verify(userMapper).updateById(concurrentUser);

        ArgumentCaptor<AuthPrincipal> principalCaptor = ArgumentCaptor.forClass(AuthPrincipal.class);
        verify(jwtService).generateToken(principalCaptor.capture());
        assertThat(principalCaptor.getValue().id()).isEqualTo(42L);
        assertThat(principalCaptor.getValue().username()).isEqualTo("13900009999");
    }
}
