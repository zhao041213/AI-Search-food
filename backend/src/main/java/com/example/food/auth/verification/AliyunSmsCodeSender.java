package com.example.food.auth.verification;

import com.aliyun.dysmsapi20170525.Client;
import com.aliyun.dysmsapi20170525.models.SendSmsRequest;
import com.aliyun.dysmsapi20170525.models.SendSmsResponse;
import com.aliyun.teaopenapi.models.Config;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@Component
@ConditionalOnProperty(prefix = "app.sms", name = "provider", havingValue = "aliyun")
public class AliyunSmsCodeSender implements SmsCodeSender {

    private static final Logger log = LoggerFactory.getLogger(AliyunSmsCodeSender.class);
    private static final String CONFIGURATION_INCOMPLETE = "Aliyun SMS configuration incomplete";
    private static final String SEND_FAILED = "短信发送失败，请稍后重试";

    private final SmsProperties.Aliyun properties;
    private final ObjectMapper objectMapper;

    public AliyunSmsCodeSender(SmsProperties smsProperties, ObjectMapper objectMapper) {
        this.properties = smsProperties.getAliyun();
        this.objectMapper = objectMapper;
        validateConfiguration();
    }

    @Override
    public SmsSendResult send(String phone, String code) {
        try {
            Config config = new Config()
                    .setAccessKeyId(properties.getAccessKeyId())
                    .setAccessKeySecret(properties.getAccessKeySecret())
                    .setEndpoint(properties.getEndpoint());
            Client client = new Client(config);
            SendSmsRequest request = new SendSmsRequest()
                    .setPhoneNumbers(phone)
                    .setSignName(properties.getSignName())
                    .setTemplateCode(properties.getTemplateCode())
                    .setTemplateParam(objectMapper.writeValueAsString(Map.of("code", code)));
            SendSmsResponse response = client.sendSms(request);
            if (response == null || response.getBody() == null) {
                log.warn("Aliyun SMS returned an empty response, phone={}", maskPhone(phone));
                throw sendFailed();
            }
            if ("OK".equals(response.getBody().getCode())) {
                log.info("Aliyun SMS accepted by provider, phone={}, requestId={}",
                        maskPhone(phone), response.getBody().getRequestId());
                return new SmsSendResult(null, 0);
            }
            log.warn("Aliyun SMS rejected by provider, phone={}, code={}, message={}, requestId={}",
                    maskPhone(phone), response.getBody().getCode(), response.getBody().getMessage(),
                    response.getBody().getRequestId());
            throw sendFailed();
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (Exception exception) {
            log.error("Aliyun SMS request failed, phone={}", maskPhone(phone), exception);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, SEND_FAILED, exception);
        }
    }

    private void validateConfiguration() {
        if (!StringUtils.hasText(properties.getAccessKeyId())
                || !StringUtils.hasText(properties.getAccessKeySecret())
                || !StringUtils.hasText(properties.getSignName())
                || !StringUtils.hasText(properties.getTemplateCode())
                || !StringUtils.hasText(properties.getEndpoint())) {
            throw new IllegalArgumentException(CONFIGURATION_INCOMPLETE);
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
}
