package com.hong.ForPaw.core.security;

import com.auth0.jwt.exceptions.SignatureVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.hong.ForPaw.domain.User.UserRole;
import com.hong.ForPaw.domain.User.User;
import com.hong.ForPaw.service.RedisService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;


import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
public class JwtAuthenticationFilter extends BasicAuthenticationFilter {

    private RedisService redisService;
    public static final Long VISIT_EXP = 1000L * 60 * 60 * 24 * 7; // 일 주일

    public JwtAuthenticationFilter(AuthenticationManager authenticationManager, RedisService redisService) {
        super(authenticationManager);
        this.redisService = redisService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
        String jwt = request.getHeader(JWTProvider.HEADER);

        if (jwt == null) {
            chain.doFilter(request, response);
            return;
        }

        try {
            DecodedJWT decodedJWT = JWTProvider.verify(jwt);

            Long id = decodedJWT.getClaim("id").asLong();
            UserRole userRole = decodedJWT.getClaim("role").as(UserRole.class);
            String nickName = decodedJWT.getClaim("nickName").asString();
            User user = User.builder().id(id).role(userRole).nickName(nickName).build();

            CustomUserDetails myUserDetails = new CustomUserDetails(user);
            Authentication authentication =
                    new UsernamePasswordAuthenticationToken(
                            myUserDetails,
                            myUserDetails.getPassword(),
                            myUserDetails.getAuthorities()
                    );

            redisService.addSetElement(createVisitKey(), id, VISIT_EXP);

            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (SignatureVerificationException sve) {
            log.error("토큰 검증 실패");
        } catch (TokenExpiredException tee) {
            log.error("토큰 만료됨");
        } finally {
            chain.doFilter(request, response);
        }
    }

    private String createVisitKey() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH");

        return "visit" + ":" + now.format(formatter);
    }
}