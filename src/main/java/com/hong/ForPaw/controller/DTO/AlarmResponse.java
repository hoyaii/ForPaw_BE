package com.hong.ForPaw.controller.DTO;

import com.hong.ForPaw.domain.Alarm.Type;
import java.time.LocalDateTime;
import java.util.List;

public class AlarmResponse {

    public record FindAlarmsDTO(List<AlarmDTO> alarms) {}

    public record AlarmDTO(Long id, String name, String content, LocalDateTime createdAt) {}
}
