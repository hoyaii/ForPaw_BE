package com.hong.forapw.config.webclient;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter @Setter
@Configuration
@ConfigurationProperties(prefix = "webclient")
public class WebClientProperties {
    private int bufferSize;
    private int connectTimeoutMillis;
    private int readTimeoutSeconds;
    private int writeTimeoutSeconds;
    private int responseTimeoutSeconds;
}