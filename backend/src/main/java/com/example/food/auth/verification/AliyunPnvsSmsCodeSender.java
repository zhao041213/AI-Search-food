package com.example.food.auth.verification;

import com.aliyun.dypnsapi20170525.Client;
import com.aliyun.dypnsapi20170525.models.CheckSmsVerifyCodeRequest;
import com.aliyun.dypnsapi20170525.models.CheckSmsVerifyCodeResponse;
import com.aliyun.dypnsapi20170525.models.SendSmsVerifyCodeRequest;
import com.aliyun.dypnsapi20170525.models.SendSmsVerifyCodeResponse;
import com.aliyun.teaopenapi.models.Config;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.Map;

@Component
@ConditionalOnProperty(prefix = "app.sms", name = "provider", havingValue = "aliyun-pnvs")
public class AliyunPnvsSmsCodeSender implements SmsCodeSender {

    private static final Logger log = LoggerFactory.getLogger(AliyunPnvsSmsCodeSender.class);
    private static final String CONFIGURATION_INCOMPLETE = "Aliyun PNVS SMS configuration incomplete";
    private static final String SEND_FAILED = "短信发送失败，请稍后重试";
    private static final String VERIFY_UNAVAILABLE = "短信验证服务暂时不可用，请稍后重试";
    private static final String VERIFICATION_INVALID = "Verification code invalid";
    private static final String COUNTRY_CODE = "86";
    private static final long NUMERIC_CODE_TYPE = 1L;
    private static final long CODE_LENGTH = 6L;
    private static final long OVERWRITE_DUPLICATE_POLICY = 1L;
    private static final long ENABLE_AUTO_RETRY = 1L;
    private static final Duration DEFAULT_CODE_EXPIRY = Duration.ofMinutes(5);
    private static final Duration DEFAULT_RESEND_INTERVAL = Duration.ofSeconds(60);

    private final SmsProperties smsProperties;
    private final SmsProperties.Pnvs properties;
    private final ObjectMapper objectMapper;
    private final ClientFactory clientFactory;

    @Autowired
    public AliyunPnvsSmsCodeSender(SmsProperties smsProperties, ObjectMapper objectMapper) {
        this(smsProperties, objectMapper, AliyunPnvsSmsCodeSender::createClient);
    }

    AliyunPnvsSmsCodeSender(
            SmsProperties smsProperties,
            ObjectMapper objectMapper,
            ClientFactory clientFactory
    ) {
        this.smsProperties = smsProperties;
        this.properties = smsProperties.getPnvs();
        this.objectMapper = objectMapper;
        this.clientFactory = clientFactory;
        validateConfiguration();
    }

    @Override
    public SmsSendResult send(String phone, String ignoredCode) {
        try {
            Client client = clientFactory.create(properties);
            SendSmsVerifyCodeRequest request = new SendSmsVerifyCodeRequest()
                    .setPhoneNumber(phone)
                    .setCountryCode(COUNTRY_CODE)
                    .setSignName(properties.getSignName())
                    .setTemplateCode(properties.getTemplateCode())
                    .setTemplateParam(templateParam())
                    .setCodeLength(CODE_LENGTH)
                    .setCodeType(NUMERIC_CODE_TYPE)
                    .setValidTime(validTimeSeconds())
                    .setInterval(intervalSeconds())
                    .setDuplicatePolicy(OVERWRITE_DUPLICATE_POLICY)
                    .setReturnVerifyCode(false)
                    .setAutoRetry(ENABLE_AUTO_RETRY);
            applySchemeName(request);

            SendSmsVerifyCodeResponse response = client.sendSmsVerifyCode(request);
            if (response == null || response.getBody() == null) {
                log.warn("Aliyun PNVS returned an empty send response, phone={}", maskPhone(phone));
                throw sendFailed();
            }
            if ("OK".equals(response.getBody().getCode())) {
                log.info("Aliyun PNVS accepted SMS, phone={}, requestId={}",
                        maskPhone(phone), response.getBody().getRequestId());
                return new SmsSendResult(null, intervalSeconds());
            }
            log.warn("Aliyun PNVS rejected SMS, phone={}, code={}, message={}, requestId={}",
                    maskPhone(phone), response.getBody().getCode(), response.getBody().getMessage(),
                    response.getBody().getRequestId());
            throw sendFailed();
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (Exception exception) {
            log.error("Aliyun PNVS SMS request failed, phone={}", maskPhone(phone), exception);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, SEND_FAILED, exception);
        }
    }

    @Override
    public boolean supportsRemoteVerification() {
        return true;
    }

    @Override
    public void verify(String phone, String code) {
        try {
            Client client = clientFactory.create(properties);
            CheckSmsVerifyCodeRequest request = new CheckSmsVerifyCodeRequest()
                    .setPhoneNumber(phone)
                    .setCountryCode(COUNTRY_CODE)
                    .setVerifyCode(code)
                    .setCaseAuthPolicy(1L);
            applySchemeName(request);

            CheckSmsVerifyCodeResponse response = client.checkSmsVerifyCode(request);
            if (response == null || response.getBody() == null) {
                log.warn("Aliyun PNVS returned an empty verify response, phone={}", maskPhone(phone));
                throw verifyUnavailable();
            }

            String verifyResult = response.getBody().getModel() == null
                    ? null
                    : response.getBody().getModel().getVerifyResult();
            if ("OK".equals(response.getBody().getCode()) && "PASS".equals(verifyResult)) {
                log.info("Aliyun PNVS verified SMS code, phone={}", maskPhone(phone));
                return;
            }
            log.warn("Aliyun PNVS rejected SMS code, phone={}, code={}, verifyResult={}, message={}",
                    maskPhone(phone), response.getBody().getCode(), verifyResult,
                    response.getBody().getMessage());
            if ("isv.ValidateFail".equals(response.getBody().getCode())
                    || ("OK".equals(response.getBody().getCode()) && "UNKNOWN".equals(verifyResult))) {
                throw new VerificationCodeException(VERIFICATION_INVALID);
            }
            throw verifyUnavailable();
        } catch (VerificationCodeException | ResponseStatusException exception) {
            throw exception;
        } catch (Exception exception) {
            log.error("Aliyun PNVS verify request failed, phone={}", maskPhone(phone), exception);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, VERIFY_UNAVAILABLE, exception);
        }
    }

    private static Client createClient(SmsProperties.Pnvs properties) throws Exception {
        Config config = new Config()
                .setAccessKeyId(properties.getAccessKeyId())
                .setAccessKeySecret(properties.getAccessKeySecret())
                .setEndpoint(properties.getEndpoint());
        return new Client(config);
    }

    private String templateParam() throws Exception {
        long validMinutes = Math.max(1, (validTimeSeconds() + 59) / 60);
        return objectMapper.writeValueAsString(Map.of(
                "code", "##code##",
                "min", String.valueOf(validMinutes)
        ));
    }

    private void applySchemeName(SendSmsVerifyCodeRequest request) {
        if (StringUtils.hasText(properties.getSchemeName())) {
            request.setSchemeName(properties.getSchemeName());
        }
    }

    private void applySchemeName(CheckSmsVerifyCodeRequest request) {
        if (StringUtils.hasText(properties.getSchemeName())) {
            request.setSchemeName(properties.getSchemeName());
        }
    }

    private long validTimeSeconds() {
        Duration expiry = smsProperties.getCodeExpiry();
        return Math.max(1, expiry == null ? DEFAULT_CODE_EXPIRY.toSeconds() : expiry.toSeconds());
    }

    private long intervalSeconds() {
        Duration interval = smsProperties.getResendInterval();
        return Math.max(1, interval == null ? DEFAULT_RESEND_INTERVAL.toSeconds() : interval.toSeconds());
    }

    private void validateConfiguration() {
        if (!StringUtils.hasText(properties.getAccessKeyId())
                || !StringUtils.hasText(properties.getAccessKeySecret())
                || !StringUtils.hasText(properties.getSignName())
                || !StringUtils.hasText(properties.getTemplateCode())
                || !StringUtils.hasText(properties.getEndpoint())) {
            throw new IllegalArgumentException(CONFIGURATION_INCOMPLETE);
        }
        if (StringUtils.hasText(properties.getSchemeName()) && properties.getSchemeName().length() > 20) {
            throw new IllegalArgumentException("Aliyun PNVS scheme name cannot exceed 20 characters");
        }
    }

    private String maskPhone(String phone) {
        if (!StringUtils.hasText(phone) || phone.length() < 7) {
            return "***";
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    private ResponseStatusException sendFailed() {
        return new ResponseStatusException(HttpStatus.BAD_GATEWAY, SEND_FAILED);
    }

    private ResponseStatusException verifyUnavailable() {
        return new ResponseStatusException(HttpStatus.BAD_GATEWAY, VERIFY_UNAVAILABLE);
    }

    @FunctionalInterface
    interface ClientFactory {

        Client create(SmsProperties.Pnvs properties) throws Exception;
    }
}
