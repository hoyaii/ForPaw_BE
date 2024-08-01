package com.hong.ForPaw.controller.DTO;

import java.time.LocalDateTime;
import java.util.List;

public class ChatResponse {

    public record SendMessageDTO(String messageId) {}

    public record FindMessageListInRoomDTO(String lastMessageId, String myNickName, List<ChatResponse.MessageDTD> messages) {}

    public record FindPreviousMessageListInRoom(List<ChatResponse.MessageDTD> messages) {}

    public record MessageDTD(String messageId,
                             String nickName,
                             String content,
                             List<ChatImageDTO> images,
                             LocalDateTime date,
                             boolean isMine) {}

    public record FindChatRoomDrawerDTO(List<ChatImageDTO> images, List<ChatUserDTO> users) {}

    public record ChatImageDTO(String imageURL) {}

    public record ChatUserDTO(Long userId,
                              String nickName,
                              String profileURL) {}

    public record FindChatRoomListDTO(List<ChatResponse.RoomDTO> rooms) {}

    public record RoomDTO(Long chatRoomId,
                          String name,
                          String lastMessageContent,
                          LocalDateTime lastMessageTime,
                          Long offset) {}

    public record FindChatRoomImagesDTO(List<ChatImageDTO> images) {}

    public record ReadMessageDTO(String id) {}
}
