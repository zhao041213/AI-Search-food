package com.example.food.admin.operation;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@ConditionalOnBean(AdminOperationLogService.class)
public class AdminOperationLogWebConfig implements WebMvcConfigurer {

    private final AdminOperationLogInterceptor interceptor;

    public AdminOperationLogWebConfig(AdminOperationLogInterceptor interceptor) {
        this.interceptor = interceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(interceptor).addPathPatterns("/api/admin/**");
    }
}
