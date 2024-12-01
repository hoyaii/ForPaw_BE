package com.hong.forapw.core.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Slf4j
public class TraceIdFilter extends OncePerRequestFilter {

    private static final String TRACE_ID_HEADER = "X-Trace-Id"; // 클라이언트와 서버 간 TraceId를 공유하기 위한 헤더 이름
    private static final String MDC_TRACE_ID_KEY = "traceId"; // MDC에서 사용할 Key 이름

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        try {
            String traceId = getOrCreateTraceId(request);
            MDC.put(MDC_TRACE_ID_KEY, traceId);

            response.setHeader(TRACE_ID_HEADER, traceId);
            chain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_TRACE_ID_KEY);
        }
    }

    private String getOrCreateTraceId(HttpServletRequest request) {
        String traceId = request.getHeader(TRACE_ID_HEADER);
        if (traceId == null || traceId.isEmpty()) {
            traceId = UUID.randomUUID().toString(); // 새 TraceId 생성
            log.debug("새 TraceId 생성: {}", traceId);
        } else {
            log.debug("기존 TraceId 사용: {}", traceId);
        }
        return traceId;
    }
}