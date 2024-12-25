package com.hong.forapw.security.filters;

import com.hong.forapw.integration.redis.RedisService;
import com.hong.forapw.common.utils.JwtUtils;
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