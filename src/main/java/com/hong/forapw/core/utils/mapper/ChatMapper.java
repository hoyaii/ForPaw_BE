package com.hong.forapw.core.utils.mapper;

import com.hong.forapw.controller.dto.ChatRequest;
import com.hong.forapw.controller.dto.ChatResponse;
import com.hong.forapw.domain.chat.ChatUser;
import com.hong.forapw.domain.chat.LinkMetadata;
import com.hong.forapw.domain.chat.Message;
import com.hong.forapw.domain.chat.MessageType;

import java.time.LocalDateTime;
import java.util.List;

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

    public static ChatResponse.MessageDTO toMessageDTO(Message message, List<ChatResponse.ChatObjectDTO> imageDTOS, Long userId){
        return new ChatResponse.MessageDTO(
                message.getId(),
                message.getNickName(),
                message.getProfileURL(),
                message.getContent(),
                message.getMessageType(),
                imageDTOS,
                message.getMetadata(),
                message.getDate(),
                message.getSenderId().equals(userId));
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
