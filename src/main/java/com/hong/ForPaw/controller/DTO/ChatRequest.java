package com.hong.ForPaw.controller.DTO;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.hong.ForPaw.domain.Chat.LinkMetadata;
import com.hong.ForPaw.domain.Chat.MessageType;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;

public class ChatRequest {

    public record SendMessageDTO(
            @NotNull(message = "채팅방 ID를 입력해주세요.")
            Long chatRoomId,
            @NotNull(message = "내용을 입력해주세요.")
            String content,
            MessageType messageType,
            List<ChatObjectDTO> objects) {}

    public record ChatObjectDTO(String objectURL){}

    public record MessageDTO(String messageId,
                             String nickName,
                             String profileURL,
                             String content,
                             MessageType messageType,
                             List<ChatObjectDTO> objects,
                             @JsonSerialize(using = LocalDateTimeSerializer.class)
                             @JsonDeserialize(using = LocalDateTimeDeserializer.class)
                             @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss.SSS")
                             LocalDateTime date,
                             Long chatRoomId,
                             Long senderId,
                             LinkMetadata linkMetadata) {}

    public record ReadMessageDTO(
            @NotNull(message = "채팅방 ID를 입력해주세요.")
            Long chatRoomId,
            @NotNull(message = "메시지 ID를 입력해주세요.")
            String messageId) {}
}
