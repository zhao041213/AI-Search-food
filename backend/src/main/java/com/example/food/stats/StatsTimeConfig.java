package com.example.food.stats;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class StatsTimeConfig {

    @Bean
    Clock statsClock() {
        return Clock.system(ZoneId.of("Asia/Shanghai"));
    }
}
