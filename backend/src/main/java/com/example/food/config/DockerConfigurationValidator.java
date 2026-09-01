package com.example.food.config;

import com.example.food.ai.qwen.QwenProperties;
import com.example.food.auth.verification.SmsProperties;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Fails fast in the container profile when required production settings are absent. */
@Component
@Profile("docker")
public class DockerConfigurationValidator {

    private final String jwtSecret;
    private final String datasourceUrl;
    private final String datasourceUsername;
    private final QwenProperties qwenProperties;
    private final SmsProperties smsProperties;

    public DockerConfigurationValidator(
            @Value("${app.jwt.secret:}") String jwtSecret,
            @Value("${spring.datasource.url:}") String datasourceUrl,
            @Value("${spring.datasource.username:}") String datasourceUsername,
            QwenProperties qwenProperties,
            SmsProperties smsProperties
    ) {
        this.jwtSecret = jwtSecret;
        this.datasourceUrl = datasourceUrl;
        this.datasourceUsername = datasourceUsername;
        this.qwenProperties = qwenProperties;
        this.smsProperties = smsProperties;
    }

    @PostConstruct
    void validate() {
        List<String> missing = new ArrayList<>();
        if (!StringUtils.hasText(jwtSecret) || jwtSecret.getBytes(StandardCharsets.UTF_8).length < 32) {
            missing.add("JWT_SECRET（至少 32 字节）");
        }
        if (!StringUtils.hasText(qwenProperties.apiKey())) {
            missing.add("DASHSCOPE_API_KEY");
        }
        if (!StringUtils.hasText(qwenProperties.endpoint())
                || !qwenProperties.endpoint().toLowerCase(Locale.ROOT).startsWith("https://")) {
            missing.add("DASHSCOPE_BASE_URL（必须使用 HTTPS）");
        }
        if (!StringUtils.hasText(datasourceUrl)
                || !datasourceUrl.toLowerCase(Locale.ROOT).startsWith("jdbc:mysql://mysql:")) {
            missing.add("MYSQL_URL（主机必须为 mysql）");
        }
        if (!StringUtils.hasText(datasourceUsername) || "root".equalsIgnoreCase(datasourceUsername)) {
            missing.add("MYSQL_USER（不能使用 root）");
        }

        if (!"aliyun-pnvs".equalsIgnoreCase(smsProperties.getProvider())) {
            missing.add("SMS_PROVIDER=aliyun-pnvs");
        } else {
            SmsProperties.Pnvs pnvs = smsProperties.getPnvs();
            if (!StringUtils.hasText(pnvs.getAccessKeyId())) {
                missing.add("ALIBABA_CLOUD_ACCESS_KEY_ID");
            }
            if (!StringUtils.hasText(pnvs.getAccessKeySecret())) {
                missing.add("ALIBABA_CLOUD_ACCESS_KEY_SECRET");
            }
            if (!StringUtils.hasText(pnvs.getSignName())) {
                missing.add("ALIYUN_PNVS_SIGN_NAME");
            }
            if (!StringUtils.hasText(pnvs.getTemplateCode())) {
                missing.add("ALIYUN_PNVS_TEMPLATE_CODE");
            }
            if (!StringUtils.hasText(pnvs.getEndpoint())) {
                missing.add("ALIYUN_PNVS_ENDPOINT");
            }
        }

        if (!missing.isEmpty()) {
            throw new IllegalStateException("Docker 生产配置缺少或不合法：" + String.join("、", missing));
        }
    }
}
