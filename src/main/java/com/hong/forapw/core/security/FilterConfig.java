package com.hong.forapw.core.security;

import com.hong.forapw.service.RedisService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FilterConfig {

    @Bean
    public TraceIdFilter traceIdFilter() {
        return new TraceIdFilter();
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(RedisService redisService, JwtUtils jwtProvider) {
        return new JwtAuthenticationFilter(redisService, jwtProvider);
    }
}
