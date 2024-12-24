package com.hong.forapw.security;


import com.hong.forapw.common.exceptions.CustomException;
import com.hong.forapw.common.exceptions.ExceptionCode;
import com.hong.forapw.common.utils.FilterResponseUtils;
import com.hong.forapw.security.filters.JwtAuthenticationFilter;
import com.hong.forapw.security.filters.TraceIdFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.security.config.annotation.web.configurers.ExceptionHandlingConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
public class SecurityConfig {

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, TraceIdFilter traceIdFilter,
                                                   JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable) // REST API에서는 CSRF 보호가 필요하지 않음
                .headers(headers ->
                        headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin)) // 동일 출처의 iframe 허용
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(sessionManagement -> // STATELESS가 JWT 인증 방식에 적합
                        sessionManagement.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .formLogin(AbstractHttpConfigurer::disable) // 폼 로그인과 기본 HTTP 인증 비활성화 (JWT 인증만 사용)
                .httpBasic(AbstractHttpConfigurer::disable)
                .exceptionHandling(this::configureExceptionHandling) // 인증-인가 실패 시 처리
                .authorizeHttpRequests(this::configureAuthorization) // 요청 경로별로 권한 제어

                .addFilterBefore(traceIdFilter, BasicAuthenticationFilter.class)
                .addFilterAfter(jwtAuthenticationFilter, BasicAuthenticationFilter.class);

        return http.build();
    }

    private CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.addAllowedHeader("*");
        configuration.addAllowedMethod("*");
        configuration.addAllowedOriginPattern("*");
        configuration.setAllowCredentials(true);
        configuration.addExposedHeader("Authorization");
        configuration.setExposedHeaders(Arrays.asList("accessToken", "refreshToken"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    private void configureExceptionHandling(ExceptionHandlingConfigurer<HttpSecurity> exceptionHandling) {
        exceptionHandling
                .authenticationEntryPoint((request, response, authException) ->
                        FilterResponseUtils.unAuthorized(response, new CustomException(ExceptionCode.USER_UNAUTHORIZED)))
                .accessDeniedHandler((request, response, accessDeniedException) ->
                        FilterResponseUtils.forbidden(response, new CustomException(ExceptionCode.USER_FORBIDDEN)));
    }

    private void configureAuthorization(AuthorizeHttpRequestsConfigurer<?>.AuthorizationManagerRequestMatcherRegistry auth) {
        authenticatedRoutes(auth);
        publicRoutes(auth);
        auth.anyRequest().authenticated();
    }

    private void authenticatedRoutes(AuthorizeHttpRequestsConfigurer<?>.AuthorizationManagerRequestMatcherRegistry auth) {
        auth
                .requestMatchers("/api/accounts/profile", "/api/accounts/password/**", "/api/accounts/role",
                        "/api/accounts/withdraw", "/api/shelters/import", "/api/animals/like",
                        "/api/accounts/withdraw/code").authenticated()
                .requestMatchers("/api/animals/*/like", "/api/animals/*/apply").authenticated();
    }

    private void publicRoutes(AuthorizeHttpRequestsConfigurer<?>.AuthorizationManagerRequestMatcherRegistry auth) {
        auth
                .requestMatchers("/api/groups/*/detail", "/api/chat/*/read").permitAll()
                .requestMatchers("/ws/**", "/api/auth/**", "/api/accounts/**", "/api/animals/**",
                        "/api/shelters/**", "/api/groups", "/api/groups/local", "/api/groups/new",
                        "/api/home", "/api/search/**", "/api/validate/accessToken",
                        "/api/posts/adoption", "/api/posts/fostering", "/api/posts/question",
                        "/api/posts/popular", "/api/groups/localAndNew", "/api/faq", "/shelters/addr").permitAll();
    }
}