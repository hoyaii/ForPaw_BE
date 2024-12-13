package com.hong.forapw.core.utils.mapper;

import com.hong.forapw.controller.dto.AlarmResponse;
import com.hong.forapw.domain.alarm.Alarm;

public class AlarmMapper {

    private AlarmMapper() {
    }

    public static AlarmResponse.AlarmDTO toAlarmDTO(Alarm alarm) {
        return new AlarmResponse.AlarmDTO(
                alarm.getId(),
                alarm.getContent(),
                alarm.getRedirectURL(),
                alarm.getCreatedDate(),
                alarm.getIsRead());
    }

    public static AlarmResponse.AlarmDTO toAlarmDTO(Alarm alarm, boolean isRead) {
        return new AlarmResponse.AlarmDTO(
                alarm.getId(),
                alarm.getContent(),
                alarm.getRedirectURL(),
                alarm.getCreatedDate(),
                isRead);
    }
}
