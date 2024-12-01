package com.hong.forapw.core.security;


import com.hong.forapw.core.errors.CustomException;
import com.hong.forapw.core.errors.ExceptionCode;
import com.hong.forapw.core.utils.FilterResponseUtils;
import com.hong.forapw.service.RedisService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.context.SecurityContextPersistenceFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
public class SecurityConfig {

    private final RedisService redisService;

    public SecurityConfig(RedisService redisService) {
        this.redisService = redisService;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public TraceIdFilter traceIdFilter() {
        return new TraceIdFilter();
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(redisService);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable) // CSRF 해제
                .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin)) // iframe 옵션 설정
                .cors(cors -> cors.configurationSource(corsConfigurationSource())) // CORS 설정
                .sessionManagement(sessionManagement -> // 세션 정책 설정
                        sessionManagement.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .formLogin(AbstractHttpConfigurer::disable) // 폼 로그인 및 비활성화
                .httpBasic(AbstractHttpConfigurer::disable) // 기본 HTTP 인증 비활성화
                .exceptionHandling(exceptionHandling -> { // 인증 예외 및 권한 부여 실패 처리자
                    exceptionHandling.authenticationEntryPoint((request, response, authException) ->
                            FilterResponseUtils.unAuthorized(response, new CustomException(ExceptionCode.USER_UNAUTHORIZED))
                    );
                    exceptionHandling.accessDeniedHandler((request, response, accessDeniedException) ->
                            FilterResponseUtils.forbidden(response, new CustomException(ExceptionCode.USER_FORBIDDEN))
                    );
                })
                .authorizeHttpRequests(auth -> auth  // 권한 설정
                        .requestMatchers("/api/accounts/profile", "/api/accounts/password/**", "/api/accounts/role", "/api/accounts/withdraw", "/api/shelters/import", "/api/animals/like", "/api/accounts/withdraw/code").authenticated()
                        .requestMatchers(new AntPathRequestMatcher("/api/animals/*/like")).authenticated()
                        .requestMatchers(new AntPathRequestMatcher("/api/animals/*/apply")).authenticated()
                        .requestMatchers(new AntPathRequestMatcher("/api/groups/*/detail")).permitAll()
                        .requestMatchers(new AntPathRequestMatcher("/api/chat/*/read")).permitAll()
                        .requestMatchers("/ws/**", "/api/auth/**", "/api/accounts/**", "/api/animals/**", "/api/shelters/**",
                                "/api/groups", "/api/groups/local", "/api/groups/new", "/api/home", "/api/search/**", "/api/validate/accessToken",
                                "/api/posts/adoption", "/api/posts/fostering", "/api/posts/question", "/api/posts/popular", "/api/groups/localAndNew", "/api/faq", "/shelters/addr").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(traceIdFilter(), BasicAuthenticationFilter.class) // TraceIdFilter
                .addFilterAfter(jwtAuthenticationFilter(), BasicAuthenticationFilter.class); // JwtAuthenticationFilter

        return http.build();
    }

    public CorsConfigurationSource corsConfigurationSource() {
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
}