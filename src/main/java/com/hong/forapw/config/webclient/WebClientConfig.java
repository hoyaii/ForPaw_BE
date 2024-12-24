package com.hong.forapw.config.webclient;


import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

@Configuration
@RequiredArgsConstructor
public class WebClientConfig {

    private final WebClientProperties properties;

    @Bean
    public WebClient webClient() {
        return WebClient.builder()
                .exchangeStrategies(createExchangeStrategies()) // 데이터 처리 전략 설정
                .clientConnector(createClientHttpConnector()) // 커넥터 설정
                .build();
    }

    private ExchangeStrategies createExchangeStrategies() {
        return ExchangeStrategies.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(properties.getBufferSize()))
                .build();
    }

    private ClientHttpConnector createClientHttpConnector() {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, properties.getConnectTimeoutMillis()) // 커넥션 타임아웃
                .doOnConnected(connection ->
                        connection.addHandlerLast(new ReadTimeoutHandler(properties.getReadTimeoutSeconds())) // 읽기 타임아웃
                                .addHandlerLast(new WriteTimeoutHandler(properties.getWriteTimeoutSeconds()))) // 쓰기 타임아웃
                .responseTimeout(Duration.ofSeconds(properties.getResponseTimeoutSeconds())); // 응답 타임아웃

        return new ReactorClientHttpConnector(httpClient);
    }
}