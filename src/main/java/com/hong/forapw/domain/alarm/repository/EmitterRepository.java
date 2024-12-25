package com.hong.forapw.domain.alarm.repository;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

public interface EmitterRepository {

    SseEmitter save(String emitterId, SseEmitter sseEmitter);

    Map<String, SseEmitter> findEmittersByMemberIdPrefix(String memberId);

    void deleteById(String id);

    void deleteEmittersByMemberIdPrefix(String memberId);
}
