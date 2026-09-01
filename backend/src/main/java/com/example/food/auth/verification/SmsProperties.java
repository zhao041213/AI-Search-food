package com.example.food.auth.verification;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@ConfigurationProperties(prefix = "app.sms")
public class SmsProperties {

    private String provider = "mock";
    private Duration codeExpiry = Duration.ofMinutes(5);
    private Duration resendInterval = Duration.ofSeconds(60);
    private int maxAttempts = 5;
    private Aliyun aliyun = new Aliyun();
    private Pnvs pnvs = new Pnvs();

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public Duration getCodeExpiry() {
        return codeExpiry;
    }

    public void setCodeExpiry(Duration codeExpiry) {
        this.codeExpiry = codeExpiry;
    }

    public Duration getResendInterval() {
        return resendInterval;
    }

    public void setResendInterval(Duration resendInterval) {
        this.resendInterval = resendInterval;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public Aliyun getAliyun() {
        return aliyun;
    }

    public void setAliyun(Aliyun aliyun) {
        this.aliyun = aliyun;
    }

    public Pnvs getPnvs() {
        return pnvs;
    }

    public void setPnvs(Pnvs pnvs) {
        this.pnvs = pnvs;
    }

    public static class Aliyun {

        private String accessKeyId = "";
        private String accessKeySecret = "";
        private String signName = "";
        private String templateCode = "";
        private String endpoint = "dysmsapi.aliyuncs.com";

        public String getAccessKeyId() {
            return accessKeyId;
        }

        public void setAccessKeyId(String accessKeyId) {
            this.accessKeyId = accessKeyId;
        }

        public String getAccessKeySecret() {
            return accessKeySecret;
        }

        public void setAccessKeySecret(String accessKeySecret) {
            this.accessKeySecret = accessKeySecret;
        }

        public String getSignName() {
            return signName;
        }

        public void setSignName(String signName) {
            this.signName = signName;
        }

        public String getTemplateCode() {
            return templateCode;
        }

        public void setTemplateCode(String templateCode) {
            this.templateCode = templateCode;
        }

        public String getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }
    }

    public static class Pnvs {

        private String accessKeyId = "";
        private String accessKeySecret = "";
        private String signName = "";
        private String templateCode = "";
        private String endpoint = "dypnsapi.aliyuncs.com";
        private String schemeName = "";
        private Duration connectTimeout = Duration.ofSeconds(5);
        private Duration readTimeout = Duration.ofSeconds(10);

        public String getAccessKeyId() {
            return accessKeyId;
        }

        public void setAccessKeyId(String accessKeyId) {
            this.accessKeyId = accessKeyId;
        }

        public String getAccessKeySecret() {
            return accessKeySecret;
        }

        public void setAccessKeySecret(String accessKeySecret) {
            this.accessKeySecret = accessKeySecret;
        }

        public String getSignName() {
            return signName;
        }

        public void setSignName(String signName) {
            this.signName = signName;
        }

        public String getTemplateCode() {
            return templateCode;
        }

        public void setTemplateCode(String templateCode) {
            this.templateCode = templateCode;
        }

        public String getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }

        public String getSchemeName() {
            return schemeName;
        }

        public void setSchemeName(String schemeName) {
            this.schemeName = schemeName;
        }

        public Duration getConnectTimeout() {
            return connectTimeout;
        }

        public void setConnectTimeout(Duration connectTimeout) {
            this.connectTimeout = connectTimeout;
        }

        public Duration getReadTimeout() {
            return readTimeout;
        }

        public void setReadTimeout(Duration readTimeout) {
            this.readTimeout = readTimeout;
        }
    }
}
