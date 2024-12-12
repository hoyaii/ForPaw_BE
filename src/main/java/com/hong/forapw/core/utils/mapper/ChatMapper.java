package com.hong.forapw.core.utils.mapper;

import com.hong.forapw.controller.dto.ChatRequest;
import com.hong.forapw.controller.dto.ChatResponse;
import com.hong.forapw.domain.chat.ChatUser;
import com.hong.forapw.domain.chat.LinkMetadata;
import com.hong.forapw.domain.chat.MessageType;

import java.time.LocalDateTime;

public class ChatMapper {

    private ChatMapper() {
    }

    public static ChatRequest.MessageDTO toMessageDTO(ChatRequest.SendMessageDTO requestDTO, String senderNickName, String messageId, LinkMetadata metadata, String profileURL, Long senderId) {
        return new ChatRequest.MessageDTO(
                messageId,
                senderNickName,
                profileURL,
                requestDTO.content(),
                (metadata != null) ? MessageType.LINK : requestDTO.messageType(),
                requestDTO.objects(),
                LocalDateTime.now(),
                requestDTO.chatRoomId(),
                senderId,
                metadata);
    }

    public static ChatResponse.RoomDTO toRoomDTO(ChatUser chatUser, String lastMessageContent, LocalDateTime lastMessageDate, long offset) {
        return new ChatResponse.RoomDTO(
                chatUser.getChatRoomId(),
                chatUser.getRoomName(),
                lastMessageContent,
                lastMessageDate,
                offset,
                chatUser.getGroupProfileURL());
    }
}
