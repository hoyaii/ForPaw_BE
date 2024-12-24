package com.hong.forapw.domain.alarm.model;

import java.time.LocalDateTime;
import java.util.List;

public class AlarmResponse {

    public record FindAlarmListDTO(List<AlarmDTO> alarms) {
    }

    public record AlarmDTO(Long id,
                           String content,
                           String redirectURL,
                           LocalDateTime date,
                           boolean isRead) {
    }
}