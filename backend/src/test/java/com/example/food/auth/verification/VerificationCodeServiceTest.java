package com.example.food.auth.verification;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
class VerificationCodeServiceTest {

    @Autowired
    private VerificationCodeService service;

    @Autowired
    private PhoneVerificationCodeMapper mapper;

    @MockBean
    private SmsCodeSender smsCodeSender;

    @SpyBean
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void cleanCodes() {
        mapper.delete(null);
        reset(smsCodeSender);
        when(smsCodeSender.send(anyString(), anyString()))
                .thenAnswer(invocation -> new SmsSendResult(invocation.getArgument(1), 0));
    }

    @Test
    void issuedMockCodeCanBeConsumedOnce() {
        String code = service.issue("13900001001", VerificationCodePurpose.REGISTER).code();

        assertThat(code).matches("\\d{6}");
        service.verify("13900001001", VerificationCodePurpose.REGISTER, code);
        assertThatThrownBy(() -> service.verify("13900001001", VerificationCodePurpose.REGISTER, code))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Verification code already used");
    }

    @Test
    void remoteProviderVerifiesCodeAndLocalRecordRemainsOneTime() {
        String phone = "13900001011";
        when(smsCodeSender.supportsRemoteVerification()).thenReturn(true);
        when(smsCodeSender.send(anyString(), anyString()))
                .thenReturn(new SmsSendResult(null, 0));

        assertThat(service.issue(phone, VerificationCodePurpose.REGISTER).code()).isNull();

        service.verify(phone, VerificationCodePurpose.REGISTER, "123456");

        verify(smsCodeSender).verify(phone, "123456");
        assertThatThrownBy(() -> service.verify(phone, VerificationCodePurpose.REGISTER, "123456"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Verification code already used");
    }

    @Test
    void remoteProviderFailureDoesNotConsumeLocalRecord() {
        String phone = "13900001012";
        when(smsCodeSender.supportsRemoteVerification()).thenReturn(true);
        when(smsCodeSender.send(anyString(), anyString()))
                .thenReturn(new SmsSendResult(null, 0));
        service.issue(phone, VerificationCodePurpose.REGISTER);
        org.mockito.Mockito.doThrow(new VerificationCodeException("Verification code invalid"))
                .when(smsCodeSender)
                .verify(phone, "000000");

        assertThatThrownBy(() -> service.verify(phone, VerificationCodePurpose.REGISTER, "000000"))
                .isInstanceOf(VerificationCodeException.class)
                .hasMessage("Verification code invalid");
        assertThat(latest(phone, VerificationCodePurpose.REGISTER).getConsumedAt()).isNull();
    }

    @Test
    void remoteProviderInvalidCodeCountsTowardLocalAttemptLimit() {
        String phone = "13900001013";
        when(smsCodeSender.supportsRemoteVerification()).thenReturn(true);
        when(smsCodeSender.send(anyString(), anyString()))
                .thenReturn(new SmsSendResult(null, 0));
        service.issue(phone, VerificationCodePurpose.REGISTER);
        org.mockito.Mockito.doThrow(new VerificationCodeException("Verification code invalid"))
                .when(smsCodeSender)
                .verify(phone, "000000");

        for (int attempt = 0; attempt < 4; attempt++) {
            assertThatThrownBy(() -> service.verify(phone, VerificationCodePurpose.REGISTER, "000000"))
                    .isInstanceOf(VerificationCodeException.class)
                    .hasMessage("Verification code invalid");
        }
        assertThatThrownBy(() -> service.verify(phone, VerificationCodePurpose.REGISTER, "000000"))
                .isInstanceOf(VerificationCodeException.class)
                .hasMessage("Too many verification attempts");
        assertThat(latest(phone, VerificationCodePurpose.REGISTER).getAttemptCount()).isEqualTo(5);
    }

    @Test
    void remoteSendFailureRetainsLocalCooldownMarker() {
        String phone = "13900001014";
        when(smsCodeSender.supportsRemoteVerification()).thenReturn(true);
        when(smsCodeSender.send(anyString(), anyString()))
                .thenThrow(new IllegalStateException("send uncertain"));

        assertThatThrownBy(() -> service.issue(phone, VerificationCodePurpose.REGISTER))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("send uncertain");
        PhoneVerificationCode failedIssue = latest(phone, VerificationCodePurpose.REGISTER);
        assertThat(failedIssue).isNotNull();
        assertThat(failedIssue.getConsumedAt()).isNotNull();
    }

    @Test
    void wrongPurposeCannotConsumeCode() {
        String code = service.issue("13900001002", VerificationCodePurpose.REGISTER).code();

        assertThatThrownBy(() -> service.verify("13900001002", VerificationCodePurpose.LOGIN, code))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Verification code invalid");
    }

    @Test
    void expiredCodeCannotBeConsumed() {
        String phone = "13900001003";
        String code = service.issue(phone, VerificationCodePurpose.REGISTER).code();
        PhoneVerificationCode stored = latest(phone, VerificationCodePurpose.REGISTER);
        stored.setExpiresAt(LocalDateTime.now().minusSeconds(1));
        mapper.updateById(stored);

        assertThatThrownBy(() -> service.verify(phone, VerificationCodePurpose.REGISTER, code))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Verification code expired");
    }

    @Test
    void resendInsideConfiguredIntervalIsRejected() {
        String phone = "13900001004";
        service.issue(phone, VerificationCodePurpose.REGISTER);

        assertThatThrownBy(() -> service.issue(phone, VerificationCodePurpose.REGISTER))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Code requested too frequently");
    }

    @Test
    void rateLimitedRequestDoesNotCalculateAnotherPasswordHash() {
        String phone = "13900001010";
        service.issue(phone, VerificationCodePurpose.REGISTER);
        clearInvocations(passwordEncoder);

        assertThatThrownBy(() -> service.issue(phone, VerificationCodePurpose.REGISTER))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Code requested too frequently");

        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    void fifthWrongAttemptLocksCode() {
        String phone = "13900001005";
        String code = service.issue(phone, VerificationCodePurpose.REGISTER).code();
        String wrongCode = code.equals("000000") ? "999999" : "000000";

        for (int attempt = 0; attempt < 4; attempt++) {
            assertThatThrownBy(() -> service.verify(phone, VerificationCodePurpose.REGISTER, wrongCode))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Verification code invalid");
        }
        assertThatThrownBy(() -> service.verify(phone, VerificationCodePurpose.REGISTER, wrongCode))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Too many verification attempts");
        assertThatThrownBy(() -> service.verify(phone, VerificationCodePurpose.REGISTER, code))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Too many verification attempts");
    }

    @Test
    void newlyIssuedCodeInvalidatesPreviousCode() {
        String phone = "13900001006";
        String firstCode = service.issue(phone, VerificationCodePurpose.REGISTER).code();
        PhoneVerificationCode firstStored = latest(phone, VerificationCodePurpose.REGISTER);
        firstStored.setCreatedAt(LocalDateTime.now().minusMinutes(2));
        mapper.updateById(firstStored);

        String secondCode = service.issue(phone, VerificationCodePurpose.REGISTER).code();

        assertThatThrownBy(() -> service.verify(phone, VerificationCodePurpose.REGISTER, firstCode))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Verification code invalid");
        service.verify(phone, VerificationCodePurpose.REGISTER, secondCode);
    }

    @Test
    void smsSenderRunsAfterDatabaseTransactionCommits() {
        AtomicBoolean transactionActive = new AtomicBoolean(true);
        when(smsCodeSender.send(anyString(), anyString())).thenAnswer(invocation -> {
            transactionActive.set(TransactionSynchronizationManager.isActualTransactionActive());
            return new SmsSendResult(invocation.getArgument(1), 0);
        });

        service.issue("13900001007", VerificationCodePurpose.REGISTER);

        assertThat(transactionActive).isFalse();
    }

    @Test
    void concurrentIssueAllowsOnlyOneCodeInsideResendInterval() throws Exception {
        String phone = "13900001008";
        int requestCount = 6;
        ExecutorService executor = Executors.newFixedThreadPool(requestCount);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<String>> futures = new ArrayList<>();

        try {
            for (int index = 0; index < requestCount; index++) {
                futures.add(executor.submit(() -> {
                    start.await();
                    try {
                        service.issue(phone, VerificationCodePurpose.REGISTER);
                        return "success";
                    } catch (IllegalArgumentException exception) {
                        return exception.getMessage();
                    }
                }));
            }
            start.countDown();

            List<String> results = new ArrayList<>();
            for (Future<String> future : futures) {
                results.add(future.get(15, TimeUnit.SECONDS));
            }

            assertThat(results).containsOnlyOnce("success");
            assertThat(results).filteredOn("Code requested too frequently"::equals)
                    .hasSize(requestCount - 1);
            assertThat(mapper.selectCount(new QueryWrapper<PhoneVerificationCode>()
                    .eq("phone", phone)
                    .eq("purpose", VerificationCodePurpose.REGISTER.name())))
                    .isEqualTo(1);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void failedSmsSendRemovesPreparedCodeAndAllowsRetry() {
        String phone = "13900001009";
        when(smsCodeSender.send(anyString(), anyString()))
                .thenThrow(new IllegalStateException("send failed"));

        assertThatThrownBy(() -> service.issue(phone, VerificationCodePurpose.REGISTER))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("send failed");
        assertThat(mapper.selectCount(new QueryWrapper<PhoneVerificationCode>()
                .eq("phone", phone)
                .eq("purpose", VerificationCodePurpose.REGISTER.name())))
                .isZero();

        reset(smsCodeSender);
        when(smsCodeSender.send(anyString(), anyString()))
                .thenAnswer(invocation -> new SmsSendResult(invocation.getArgument(1), 0));
        assertThat(service.issue(phone, VerificationCodePurpose.REGISTER).code())
                .matches("\\d{6}");
    }

    private PhoneVerificationCode latest(String phone, VerificationCodePurpose purpose) {
        return mapper.selectOne(new QueryWrapper<PhoneVerificationCode>()
                .eq("phone", phone)
                .eq("purpose", purpose.name())
                .orderByDesc("id")
                .last("LIMIT 1"));
    }
}
