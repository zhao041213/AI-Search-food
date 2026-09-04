package com.example.food.admin.user;

import com.example.food.admin.user.dto.AdminUserStatusRequest;
import com.example.food.review.UploadedFileMapper;
import com.example.food.user.User;
import com.example.food.user.UserMapper;
import com.example.food.user.account.UserAvatarFileStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.isNull;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private UploadedFileMapper uploadedFileMapper;

    @Mock
    private UserAvatarFileStorage avatarStorage;

    private AdminUserService service;

    @BeforeEach
    void setUp() {
        service = new AdminUserService(userMapper, uploadedFileMapper, avatarStorage);
    }

    @Test
    void listsUsersWithSafePageArgumentsAndMaskedPhone() {
        User user = user(7L, "小明", "13800138000", true);
        when(userMapper.findAdminPage(isNull(), eq(true), eq(false), any(), eq(100), eq(0))).thenReturn(List.of(user));
        when(userMapper.countAdminUsers(isNull(), eq(true), eq(false), any())).thenReturn(1L);

        var page = service.list(" ", true, false, 500, -10);

        assertThat(page.total()).isEqualTo(1L);
        assertThat(page.limit()).isEqualTo(100);
        assertThat(page.offset()).isZero();
        assertThat(page.items()).singleElement().satisfies(item -> {
            assertThat(item.phone()).isEqualTo("138****8000");
            assertThat(item.passwordLocked()).isFalse();
        });
    }

    @Test
    void disablingUserIncrementsAuthVersionAndRepeatedDisableHasNoSideEffect() {
        User user = user(7L, "小明", "13800138000", true);
        user.setAuthVersion(4);
        when(userMapper.selectByIdForUpdate(7L)).thenReturn(user);
        when(userMapper.updateAdminStatus(user)).thenReturn(1);

        var result = service.updateStatus(7L, new AdminUserStatusRequest(false, "违反社区规则"));

        assertThat(result.changed()).isTrue();
        assertThat(user.getEnabled()).isFalse();
        assertThat(user.getAuthVersion()).isEqualTo(5);
        verify(userMapper).updateAdminStatus(user);

        var repeated = service.updateStatus(7L, new AdminUserStatusRequest(false, "重复操作"));

        assertThat(repeated.changed()).isFalse();
        verify(userMapper).updateAdminStatus(user);
    }

    @Test
    void rejectsBlankDisableReasonBeforeLoadingUser() {
        assertThatThrownBy(() -> service.updateStatus(7L, new AdminUserStatusRequest(false, "  ")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("禁用原因不能为空");

        verify(userMapper, never()).selectByIdForUpdate(any());
    }

    @Test
    void clearsExpiredPasswordLockWithoutChangingPasswordOrAuthVersion() {
        User user = user(7L, "小明", "13800138000", true);
        user.setAuthVersion(3);
        user.setPasswordHash("hash");
        user.setPasswordFailedAttempts(5);
        user.setPasswordLockedUntil(LocalDateTime.now().minusMinutes(1));
        when(userMapper.selectByIdForUpdate(7L)).thenReturn(user);
        when(userMapper.clearPasswordLock(any(), any())).thenReturn(1);

        var result = service.clearPasswordLock(7L);

        assertThat(result.changed()).isTrue();
        assertThat(user.getPasswordHash()).isEqualTo("hash");
        assertThat(user.getAuthVersion()).isEqualTo(3);
        assertThat(user.getPasswordFailedAttempts()).isZero();
        assertThat(user.getPasswordLockedUntil()).isNull();
        assertThat(result.user().enabled()).isTrue();
    }

    private User user(Long id, String nickname, String phone, boolean enabled) {
        User user = new User();
        user.setId(id);
        user.setNickname(nickname);
        user.setPhone(phone);
        user.setRole("USER");
        user.setEnabled(enabled);
        user.setCreatedAt(LocalDateTime.now().minusDays(1));
        user.setLastLoginAt(LocalDateTime.now().minusHours(1));
        return user;
    }
}
