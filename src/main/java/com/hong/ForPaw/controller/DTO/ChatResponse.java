package com.hong.ForPaw.controller.DTO;

import com.hong.ForPaw.domain.Chat.MessageType;

import java.time.LocalDateTime;
import java.util.List;

public class ChatResponse {

    public record SendMessageDTO(String messageId) {}

    public record FindMessageListInRoomDTO(String chatRoomName, String lastMessageId, String myNickName, List<MessageDTO> messages) {}

    public record MessageDTO(String messageId,
                             String nickName,
                             String profileURL,
                             String content,
                             MessageType messageType,
                             List<ChatObjectDTO> objects,
                             LocalDateTime date,
                             boolean isMine) {}

    public record FindChatRoomDrawerDTO(List<DrawerObjectDTO> images, List<DrawerObjectDTO> files, List<ChatUserDTO> users) {}

    public record ChatObjectDTO(String objectURL) {}

    public record DrawerObjectDTO(String messageId,
                                  String nickName,
                                  String profileURL,
                                  List<ChatObjectDTO> objects,
                                  LocalDateTime date){}

    public record ChatUserDTO(Long userId,
                              String nickName,
                              String profileURL) {}

    public record FindChatRoomListDTO(List<ChatResponse.RoomDTO> rooms) {}

    public record RoomDTO(Long chatRoomId,
                          String name,
                          String lastMessageContent,
                          LocalDateTime lastMessageTime,
                          Long offset) {}

    public record FindChatRoomObjectsDTO(List<DrawerObjectDTO> images, List<DrawerObjectDTO> files) {}

    public record ReadMessageDTO(String id) {}
}
