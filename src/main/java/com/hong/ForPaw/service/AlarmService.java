package com.hong.ForPaw.service;

import com.hong.ForPaw.repository.AlarmRepository;
import com.hong.ForPaw.repository.EmitterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AlarmService {

    private final AlarmRepository alarmRepository;
    private final EmitterRepository emitterRepository;

    private static final Long DEFAULT_TIMEOUT = 60L * 1000 * 60;

    @Transactional
    public SseEmitter connectToAlarm(String username, String lastEventId) {
        // SseEmitter 객체 생성
        String emitterId = generateIdByTime(username);
        SseEmitter emitter = emitterRepository.save(emitterId, new SseEmitter(DEFAULT_TIMEOUT));

        // 연결 종료 시 이벤트 리소스 정리
        emitter.onCompletion(() -> emitterRepository.deleteById(emitterId));
        emitter.onTimeout(() -> emitterRepository.deleteById(emitterId));

        // 503 에러를 방지하기 위한 더미 이벤트 전송
        String eventId = generateIdByTime(username);
        sendNotification(emitter, eventId, emitterId, "EventStream Created. [userEmail=" + username + "]");

        // 클라이언트가 미수신한 이벤트 처리
        if (hasUnreceivedAlarm(lastEventId)) {
            sendMissingAlarm(lastEventId, username, emitterId, emitter);
        }

        return emitter;
    }

    private void sendNotification(SseEmitter emitter, String eventId, String emitterId, Object data) {
        // SseEmitter 객체를 통해 클라이언트에게 알람 이벤트를 전송
        try {
            emitter.send(SseEmitter.event()
                    .id(eventId)
                    .name("sse")
                    .data(data)
            );
        } catch (IOException exception) {
            emitterRepository.deleteById(emitterId);
        }
    }

    private void sendMissingAlarm(String lastEventId, String userEmail, String emitterId, SseEmitter emitter) {
        // 클라이언트가 미수신한 이벤트 목록을 전송
        Map<String, Object> eventCaches = emitterRepository.findAllEventCacheStartWithMemberId(String.valueOf(userEmail));
        eventCaches.entrySet().stream()
                .filter(entry -> lastEventId.compareTo(entry.getKey()) < 0)
                .forEach(entry -> sendNotification(emitter, entry.getKey(), emitterId, entry.getValue()));
    }

    private boolean hasUnreceivedAlarm(String lastEventId) {
        return !lastEventId.isEmpty();
    }

    private String generateIdByTime(String email) {
        return email + "_" + System.currentTimeMillis();
    }

}