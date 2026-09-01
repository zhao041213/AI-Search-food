package com.example.food.auth.verification;

import com.aliyun.dypnsapi20170525.Client;
import com.aliyun.dypnsapi20170525.models.CheckSmsVerifyCodeRequest;
import com.aliyun.dypnsapi20170525.models.CheckSmsVerifyCodeResponse;
import com.aliyun.dypnsapi20170525.models.CheckSmsVerifyCodeResponseBody;
import com.aliyun.dypnsapi20170525.models.SendSmsVerifyCodeRequest;
import com.aliyun.dypnsapi20170525.models.SendSmsVerifyCodeResponse;
import com.aliyun.dypnsapi20170525.models.SendSmsVerifyCodeResponseBody;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AliyunPnvsSmsCodeSenderTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void missingRequiredConfigurationIsRejected() {
        List<Consumer<SmsProperties.Pnvs>> missingConfigurations = List.of(
                config -> config.setAccessKeyId(""),
                config -> config.setAccessKeySecret(""),
                config -> config.setSignName(""),
                config -> config.setTemplateCode(""),
                config -> config.setEndpoint("")
        );

        for (Consumer<SmsProperties.Pnvs> removeRequiredValue : missingConfigurations) {
            SmsProperties properties = completeProperties();
            removeRequiredValue.accept(properties.getPnvs());

            assertThatThrownBy(() -> new AliyunPnvsSmsCodeSender(
                    properties,
                    objectMapper,
                    ignored -> mock(Client.class)
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Aliyun PNVS SMS configuration incomplete");
        }
    }

    @Test
    void httpEndpointIsRejectedForRemoteSms() {
        SmsProperties properties = completeProperties();
        properties.getPnvs().setEndpoint("http://dypnsapi.aliyuncs.com");

        assertThatThrownBy(() -> new AliyunPnvsSmsCodeSender(
                properties,
                objectMapper,
                ignored -> mock(Client.class)
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Aliyun PNVS endpoint must use HTTPS");
    }

    @Test
    void sendUsesProviderGeneratedCodeAndConfiguredLimits() throws Exception {
        Client client = mock(Client.class);
        when(client.sendSmsVerifyCode(any(SendSmsVerifyCodeRequest.class)))
                .thenReturn(new SendSmsVerifyCodeResponse()
                        .setBody(new SendSmsVerifyCodeResponseBody()
                                .setCode("OK")
                                .setRequestId("send-request")));
        SmsProperties properties = completeProperties();
        properties.setCodeExpiry(Duration.ofMinutes(2));
        properties.setResendInterval(Duration.ofSeconds(15));
        AliyunPnvsSmsCodeSender sender = new AliyunPnvsSmsCodeSender(
                properties,
                objectMapper,
                ignored -> client
        );

        SmsSendResult result = sender.send("13800138000", "local-code-is-ignored");

        var requestCaptor = org.mockito.ArgumentCaptor.forClass(SendSmsVerifyCodeRequest.class);
        verify(client).sendSmsVerifyCode(requestCaptor.capture());
        SendSmsVerifyCodeRequest request = requestCaptor.getValue();
        JsonNode templateParam = objectMapper.readTree(request.getTemplateParam());
        assertThat(request.getPhoneNumber()).isEqualTo("13800138000");
        assertThat(request.getCountryCode()).isEqualTo("86");
        assertThat(request.getSignName()).isEqualTo("PNVS system sign");
        assertThat(request.getTemplateCode()).isEqualTo("100001");
        assertThat(templateParam.path("code").asText()).isEqualTo("##code##");
        assertThat(templateParam.path("min").asText()).isEqualTo("2");
        assertThat(request.getCodeLength()).isEqualTo(6L);
        assertThat(request.getCodeType()).isEqualTo(1L);
        assertThat(request.getValidTime()).isEqualTo(120L);
        assertThat(request.getInterval()).isEqualTo(15L);
        assertThat(request.getDuplicatePolicy()).isEqualTo(1L);
        assertThat(request.getReturnVerifyCode()).isFalse();
        assertThat(result.code()).isNull();
        assertThat(result.retryAfterSeconds()).isEqualTo(15L);
    }

    @Test
    void verifyAcceptsOnlyPassResult() throws Exception {
        Client client = mock(Client.class);
        when(client.checkSmsVerifyCode(any(CheckSmsVerifyCodeRequest.class)))
                .thenReturn(new CheckSmsVerifyCodeResponse()
                        .setBody(new CheckSmsVerifyCodeResponseBody()
                                .setCode("OK")
                                .setModel(new CheckSmsVerifyCodeResponseBody.CheckSmsVerifyCodeResponseBodyModel()
                                        .setVerifyResult("PASS"))));
        SmsProperties properties = completeProperties();
        properties.getPnvs().setSchemeName("recipe-login");
        AliyunPnvsSmsCodeSender sender = new AliyunPnvsSmsCodeSender(
                properties,
                objectMapper,
                ignored -> client
        );

        sender.verify("13800138000", "123456");

        var requestCaptor = org.mockito.ArgumentCaptor.forClass(CheckSmsVerifyCodeRequest.class);
        verify(client).checkSmsVerifyCode(requestCaptor.capture());
        CheckSmsVerifyCodeRequest request = requestCaptor.getValue();
        assertThat(request.getPhoneNumber()).isEqualTo("13800138000");
        assertThat(request.getCountryCode()).isEqualTo("86");
        assertThat(request.getVerifyCode()).isEqualTo("123456");
        assertThat(request.getCaseAuthPolicy()).isEqualTo(1L);
        assertThat(request.getSchemeName()).isEqualTo("recipe-login");
    }

    @Test
    void unknownVerifyResultIsReportedAsInvalidCode() throws Exception {
        Client client = mock(Client.class);
        when(client.checkSmsVerifyCode(any(CheckSmsVerifyCodeRequest.class)))
                .thenReturn(new CheckSmsVerifyCodeResponse()
                        .setBody(new CheckSmsVerifyCodeResponseBody()
                                .setCode("OK")
                                .setModel(new CheckSmsVerifyCodeResponseBody.CheckSmsVerifyCodeResponseBodyModel()
                                        .setVerifyResult("UNKNOWN"))));
        AliyunPnvsSmsCodeSender sender = new AliyunPnvsSmsCodeSender(
                completeProperties(),
                objectMapper,
                ignored -> client
        );

        assertThatThrownBy(() -> sender.verify("13800138000", "000000"))
                .isInstanceOf(VerificationCodeException.class)
                .hasMessage("Verification code invalid");
    }

    private SmsProperties completeProperties() {
        SmsProperties properties = new SmsProperties();
        properties.setProvider("aliyun-pnvs");
        properties.getPnvs().setAccessKeyId("test-access-key-id");
        properties.getPnvs().setAccessKeySecret("test-access-key-secret");
        properties.getPnvs().setSignName("PNVS system sign");
        properties.getPnvs().setTemplateCode("100001");
        return properties;
    }
}
