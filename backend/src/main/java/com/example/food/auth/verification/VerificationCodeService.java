package com.example.food.auth.verification;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
public class VerificationCodeService {

    private static final int CODE_BOUND = 1_000_000;

    private final PhoneVerificationCodeMapper mapper;
    private final PhoneVerificationCodeLockMapper lockMapper;
    private final PasswordEncoder passwordEncoder;
    private final SmsCodeSender smsCodeSender;
    private final SmsProperties properties;
    private final TransactionTemplate transactionTemplate;
    private final SecureRandom secureRandom = new SecureRandom();

    public VerificationCodeService(
            PhoneVerificationCodeMapper mapper,
            PhoneVerificationCodeLockMapper lockMapper,
            PasswordEncoder passwordEncoder,
            SmsCodeSender smsCodeSender,
            SmsProperties properties,
            TransactionTemplate transactionTemplate
    ) {
        this.mapper = mapper;
        this.lockMapper = lockMapper;
        this.passwordEncoder = passwordEncoder;
        this.smsCodeSender = smsCodeSender;
        this.properties = properties;
        this.transactionTemplate = transactionTemplate;
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public SmsSendResult issue(String phone, VerificationCodePurpose purpose) {
        ensureResendAllowed(latest(phone, purpose), LocalDateTime.now());

        String code = "%06d".formatted(secureRandom.nextInt(CODE_BOUND));
        String codeHash = passwordEncoder.encode(code);
        PreparedCode preparedCode = transactionTemplate.execute(status ->
                prepareIssue(phone, purpose, code, codeHash));
        if (preparedCode == null) {
            throw new IllegalStateException("Verification code could not be created");
        }

        SmsSendResult result;
        try {
            result = smsCodeSender.send(phone, preparedCode.code());
        } catch (RuntimeException exception) {
            cleanupFailedIssue(preparedCode.id(), exception);
            throw exception;
        }
        return new SmsSendResult(
                result.code(),
                Math.max(0, properties.getResendInterval().toSeconds())
        );
    }

    private PreparedCode prepareIssue(
            String phone,
            VerificationCodePurpose purpose,
            String code,
            String codeHash
    ) {
        lockMapper.ensureLockRow(phone, purpose.name());
        lockMapper.lock(phone, purpose.name());

        LocalDateTime now = LocalDateTime.now();
        PhoneVerificationCode latest = latest(phone, purpose);
        ensureResendAllowed(latest, now);

        mapper.update(null, new UpdateWrapper<PhoneVerificationCode>()
                .eq("phone", phone)
                .eq("purpose", purpose.name())
                .isNull("consumed_at")
                .set("consumed_at", now));

        PhoneVerificationCode verificationCode = new PhoneVerificationCode();
        verificationCode.setPhone(phone);
        verificationCode.setPurpose(purpose.name());
        verificationCode.setCodeHash(codeHash);
        verificationCode.setExpiresAt(now.plus(properties.getCodeExpiry()));
        verificationCode.setAttemptCount(0);
        verificationCode.setMaxAttempts(properties.getMaxAttempts());
        verificationCode.setCreatedAt(now);
        mapper.insert(verificationCode);

        return new PreparedCode(verificationCode.getId(), code);
    }

    private void cleanupFailedIssue(Long verificationCodeId, RuntimeException originalException) {
        try {
            transactionTemplate.executeWithoutResult(status ->
                    mapper.delete(new QueryWrapper<PhoneVerificationCode>()
                            .eq("id", verificationCodeId)
                            .isNull("consumed_at")));
        } catch (RuntimeException cleanupException) {
            originalException.addSuppressed(cleanupException);
        }
    }

    public void verify(String phone, VerificationCodePurpose purpose, String submittedCode) {
        VerificationResult result = transactionTemplate.execute(status ->
                verifyInTransaction(phone, purpose, submittedCode));
        if (result == null) {
            throw new VerificationCodeException("Verification code invalid");
        }
        if (result.errorMessage() != null) {
            throw new VerificationCodeException(result.errorMessage());
        }
    }

    private VerificationResult verifyInTransaction(
            String phone,
            VerificationCodePurpose purpose,
            String submittedCode
    ) {
        PhoneVerificationCode verificationCode = latest(phone, purpose);
        LocalDateTime now = LocalDateTime.now();
        String currentError = validationError(verificationCode, now);
        if (currentError != null) {
            return VerificationResult.error(currentError);
        }
        if (!passwordEncoder.matches(submittedCode, verificationCode.getCodeHash())) {
            return VerificationResult.error(recordFailedAttempt(verificationCode.getId()));
        }

        int updatedRows = mapper.update(null, new UpdateWrapper<PhoneVerificationCode>()
                .eq("id", verificationCode.getId())
                .eq("attempt_count", verificationCode.getAttemptCount())
                .isNull("consumed_at")
                .gt("expires_at", now)
                .lt("attempt_count", verificationCode.getMaxAttempts())
                .set("consumed_at", now));
        if (updatedRows == 1) {
            return VerificationResult.success();
        }

        String refreshedError = validationError(mapper.selectById(verificationCode.getId()), now);
        return VerificationResult.error(
                refreshedError == null ? "Verification code invalid" : refreshedError
        );
    }

    private String recordFailedAttempt(Long verificationCodeId) {
        PhoneVerificationCode verificationCode = mapper.selectById(verificationCodeId);
        LocalDateTime now = LocalDateTime.now();
        String currentError = validationError(verificationCode, now);
        if (currentError != null) {
            return currentError;
        }

        int updatedRows = mapper.update(null, new UpdateWrapper<PhoneVerificationCode>()
                .eq("id", verificationCodeId)
                .isNull("consumed_at")
                .lt("attempt_count", verificationCode.getMaxAttempts())
                .setSql("attempt_count = attempt_count + 1"));
        if (updatedRows != 1) {
            String refreshedError = validationError(mapper.selectById(verificationCodeId), now);
            return refreshedError == null ? "Verification code invalid" : refreshedError;
        }
        PhoneVerificationCode refreshed = mapper.selectById(verificationCodeId);
        return refreshed.getAttemptCount() >= refreshed.getMaxAttempts()
                ? "Too many verification attempts"
                : "Verification code invalid";
    }

    private String validationError(PhoneVerificationCode verificationCode, LocalDateTime now) {
        if (verificationCode == null) {
            return "Verification code invalid";
        }
        if (verificationCode.getConsumedAt() != null) {
            return "Verification code already used";
        }
        if (!verificationCode.getExpiresAt().isAfter(now)) {
            return "Verification code expired";
        }
        if (verificationCode.getAttemptCount() >= verificationCode.getMaxAttempts()) {
            return "Too many verification attempts";
        }
        return null;
    }

    private void ensureResendAllowed(PhoneVerificationCode verificationCode, LocalDateTime now) {
        if (verificationCode != null
                && verificationCode.getCreatedAt() != null
                && now.isBefore(verificationCode.getCreatedAt().plus(properties.getResendInterval()))) {
            throw new IllegalArgumentException("Code requested too frequently");
        }
    }

    private PhoneVerificationCode latest(String phone, VerificationCodePurpose purpose) {
        return mapper.selectOne(new QueryWrapper<PhoneVerificationCode>()
                .eq("phone", phone)
                .eq("purpose", purpose.name())
                .orderByDesc("id")
                .last("LIMIT 1"));
    }

    private record VerificationResult(String errorMessage) {

        private static VerificationResult success() {
            return new VerificationResult(null);
        }

        private static VerificationResult error(String errorMessage) {
            return new VerificationResult(errorMessage);
        }
    }

    private record PreparedCode(Long id, String code) {
    }
}
