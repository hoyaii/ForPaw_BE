package com.hong.forapw.controller.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.hong.forapw.domain.alarm.AlarmType;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public class AlarmRequest {

    public record ReadAlarmDTO(@NotNull Long id) {
    }

    public record AlarmDTO(Long receiverId,
                           String content,
                           String redirectURL,
                           @JsonProperty("meetDate")
                           @JsonSerialize(using = LocalDateTimeSerializer.class)
                           @JsonDeserialize(using = LocalDateTimeDeserializer.class)
                           @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss.SSS")
                           LocalDateTime date,
                           AlarmType alarmType) {
    }
}
