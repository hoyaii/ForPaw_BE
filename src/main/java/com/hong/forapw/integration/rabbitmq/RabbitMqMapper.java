package com.hong.forapw.integration.rabbitmq;

import com.hong.forapw.domain.alarm.model.AlarmRequest;
import com.hong.forapw.domain.chat.model.ChatRequest;
import com.hong.forapw.domain.alarm.entity.Alarm;
import com.hong.forapw.domain.alarm.constant.AlarmType;
import com.hong.forapw.domain.chat.entity.Message;
import com.hong.forapw.domain.user.entity.User;

import java.time.LocalDateTime;
import java.util.List;

public class RabbitMqMapper {

    private RabbitMqMapper() {
    }

    public static Message buildMessage(ChatRequest.MessageDTO messageDTO, List<String> objectURLs) {
        return Message.builder()
                .id(messageDTO.messageId())
                .nickName(messageDTO.nickName())
                .profileURL(messageDTO.profileURL())
                .content(messageDTO.content())
                .messageType(messageDTO.messageType())
                .objectURLs(objectURLs)
                .date(messageDTO.date())
                .chatRoomId(messageDTO.chatRoomId())
                .senderId(messageDTO.senderId())
                .metadata(messageDTO.linkMetadata())
                .build();
    }

    public static Alarm buildAlarm(AlarmRequest.AlarmDTO alarmDTO, User receiver) {
        return Alarm.builder()
                .receiver(receiver)
                .content(alarmDTO.content())
                .redirectURL(alarmDTO.redirectURL())
                .alarmType(alarmDTO.alarmType())
                .build();
    }

    public static AlarmRequest.AlarmDTO toAlarmDTO(User user, String content, String redirectURL) {
        return new AlarmRequest.AlarmDTO(
                user.getId(),
                content,
                redirectURL,
                LocalDateTime.now(),
                AlarmType.CHATTING);
    }
}
