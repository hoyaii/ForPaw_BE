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
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
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

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, TraceIdFilter traceIdFilter, JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin)) // iframe 옵션 설정
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(sessionManagement ->
                        sessionManagement.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable) // 기본 HTTP 인증 비활성화

                .exceptionHandling(exceptionHandling -> {
                    exceptionHandling.authenticationEntryPoint((request, response, authException) ->
                            FilterResponseUtils.unAuthorized(response, new CustomException(ExceptionCode.USER_UNAUTHORIZED))
                    );
                    exceptionHandling.accessDeniedHandler((request, response, accessDeniedException) ->
                            FilterResponseUtils.forbidden(response, new CustomException(ExceptionCode.USER_FORBIDDEN))
                    );
                })

                .authorizeHttpRequests(auth -> {
                    authenticatedRoutes(auth);
                    publicRoutes(auth);
                    auth.anyRequest().authenticated();
                })

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