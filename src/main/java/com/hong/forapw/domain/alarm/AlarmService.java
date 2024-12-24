package com.hong.forapw.domain.alarm;

import com.hong.forapw.domain.alarm.model.AlarmRequest;
import com.hong.forapw.domain.alarm.model.AlarmResponse;
import com.hong.forapw.common.exceptions.CustomException;
import com.hong.forapw.common.exceptions.ExceptionCode;
import com.hong.forapw.domain.alarm.entity.Alarm;
import com.hong.forapw.domain.alarm.constant.AlarmType;
import com.hong.forapw.domain.alarm.repository.AlarmRepository;
import com.hong.forapw.domain.alarm.repository.EmitterRepository;
import com.hong.forapw.integration.rabbitmq.RabbitMqUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static com.hong.forapw.domain.alarm.AlarmMapper.toAlarmDTO;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AlarmService {

    private final AlarmRepository alarmRepository;
    private final EmitterRepository emitterRepository;
    private final RabbitMqUtils brokerService;

    private static final Long DEFAULT_TIMEOUT = 60L * 1000 * 60;
    private static final String SSE_EVENT_NAME = "sse";

    @Transactional
    @Scheduled(cron = "0 30 1 * * *")
    public void cleanUpAlarms() {
        LocalDateTime oneWeekAgo = LocalDateTime.now().minusWeeks(1);
        alarmRepository.deleteReadAlarmBefore(oneWeekAgo);

        LocalDateTime oneMonthAgo = LocalDateTime.now().minusMonths(1);
        alarmRepository.deleteNotReadAlarmBefore(oneMonthAgo);
    }

    @Transactional
    public SseEmitter connectToSseForAlarms(String userId) {
        String emitterId = createTimestampedId(userId);
        SseEmitter emitter = emitterRepository.save(emitterId, new SseEmitter(DEFAULT_TIMEOUT));

        handleEmitterCompletion(emitter, emitterId);
        sendKeepAliveEvent(userId, emitter, emitterId);

        return emitter;
    }

    public void sendAlarmToBroker(Long userId, String content, String redirectURL, AlarmType alarmType) {
        AlarmRequest.AlarmDTO alarmDTO = new AlarmRequest.AlarmDTO(
                userId,
                content,
                redirectURL,
                LocalDateTime.now(),
                alarmType);
        brokerService.sendAlarmToUser(userId, alarmDTO);
    }

    public AlarmResponse.FindAlarmListDTO findAlarms(Long userId) {
        List<Alarm> alarms = alarmRepository.findByReceiverId(userId);
        if (alarms.isEmpty()) {
            throw new CustomException(ExceptionCode.ALARM_NOT_EXIST);
        }

        List<AlarmResponse.AlarmDTO> alarmDTOS = alarms.stream()
                .map(AlarmMapper::toAlarmDTO)
                .toList();

        return new AlarmResponse.FindAlarmListDTO(alarmDTOS);
    }

    @Transactional
    public void updateAlarmAsRead(Long alarmId, Long userId) {
        Alarm alarm = alarmRepository.findById(alarmId).orElseThrow(
                () -> new CustomException(ExceptionCode.ALARM_NOT_FOUND)
        );

        validateAlarmAuthorization(userId, alarm);
        alarm.updateIsRead(true, LocalDateTime.now());
    }

    // 503 에러를 방지하기 위해 더미 이벤트 전송
    private void sendKeepAliveEvent(String userId, SseEmitter emitter, String emitterId) {
        String eventId = createTimestampedId(userId);
        emitAlarmEvent(emitter, eventId, emitterId, "ForPaw");
    }

    private void handleEmitterCompletion(SseEmitter emitter, String emitterId) {
        emitter.onCompletion(() -> emitterRepository.deleteById(emitterId));
        emitter.onTimeout(() -> emitterRepository.deleteById(emitterId));
    }

    private void validateAlarmAuthorization(Long userId, Alarm alarm) {
        if (!alarm.getReceiverId().equals(userId)) {
            throw new CustomException(ExceptionCode.USER_FORBIDDEN);
        }
    }

    private void emitAlarmEvent(SseEmitter emitter, String eventId, String emitterId, Object data) {
        try {
            emitter.send(SseEmitter.event()
                    .id(eventId)
                    .name(SSE_EVENT_NAME)
                    .data(data)
            );
        } catch (IOException e) {
            log.error("SSE 이벤트 전송 실패, emitterId: {}", emitterId, e);
            emitterRepository.deleteById(emitterId);
        }
    }

    private String createTimestampedId(String userId) {
        return userId + "_" + System.currentTimeMillis();
    }
}