package com.hong.forapw.core.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ObjectMapperConfig {

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false) // 알 수 없는 속성 무시
                .setSerializationInclusion(JsonInclude.Include.NON_NULL) // null 값 제외
                .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE) // Snake Case 지원
                .registerModule(new JavaTimeModule()) // 날짜-시간 포맷 지정
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS) // Timestamp 대신 ISO 8601 사용
                .enable(SerializationFeature.INDENT_OUTPUT); // 출력 예쁘게 나오는 거
    }
}
