package com.hong.ForPaw.service;

import com.hong.ForPaw.controller.DTO.ChatRequest;
import com.hong.ForPaw.controller.DTO.ChatResponse;
import com.hong.ForPaw.core.errors.CustomException;
import com.hong.ForPaw.core.errors.ExceptionCode;
import com.hong.ForPaw.domain.Chat.ChatUser;
import com.hong.ForPaw.domain.Chat.Message;
import com.hong.ForPaw.repository.Chat.ChatRoomRepository;
import com.hong.ForPaw.repository.Chat.ChatUserRepository;
import com.hong.ForPaw.repository.Chat.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatService {

    private final MessageRepository messageRepository;
    private final ChatUserRepository chatUserRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final RabbitTemplate rabbitTemplate;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public void sendMessage(ChatRequest.SendMessageDTO requestDTO, Long senderId, String senderName){
        // 권한 체크
        checkChatAuthority(senderId, requestDTO.chatRoomId());

        // 우선 메시지 DB에 저장
        LocalDateTime date = LocalDateTime.now();

        Message message = Message.builder()
                .chatRoomId(requestDTO.chatRoomId())
                .senderId(senderId)
                .senderName(senderName)
                .content(requestDTO.content())
                .date(date)
                .build();

        messageRepository.save(message);

        // 전송을 위한 메시지 DTO
        ChatRequest.MessageDTO messageDTO = new ChatRequest.MessageDTO(message.getId(), senderId, senderName, requestDTO.content(), date);

        // 메시지 브로커에 전송
        String exchangeName = "chatroom." + requestDTO.chatRoomId() + ".exchange";
        rabbitTemplate.convertAndSend(exchangeName, "", messageDTO);

        // STOMP 프로토콜로 실시간으로 메시지 전송 (for 화면)
        String destination = "/topic/chatRoom." + requestDTO.chatRoomId();
        messagingTemplate.convertAndSend(destination, messageDTO);
    }

    @Transactional
    public ChatResponse.FindChatRoomListDTO findChatRoomList(Long userId){
        // chatRoom을 패치조인
        List<ChatUser> chatUsers = chatUserRepository.findByUserIdWithChatRoom(userId);

        List<ChatResponse.RoomDTO> roomDTOS = chatUsers.stream()
                .map(chatUser -> {
                    // 마지막으로 읽은 메시지
                    Optional<Message> lastMessageOP = messageRepository.findById(chatUser.getLastMessageId());
                    String lastMessageContent = lastMessageOP.map(Message::getContent).orElse(null);
                    LocalDateTime lastMessageDate = lastMessageOP.map(Message::getDate).orElse(null);

                    // 마지막으로 읽은 페이지
                    Long lastReadMessageIdx = chatUser.getLastMessageIdx();
                    Long offset = lastReadMessageIdx != 0L ? lastReadMessageIdx / 50 : 0L;

                    return new ChatResponse.RoomDTO(
                            chatUser.getChatRoom().getId(),
                            chatUser.getChatRoom().getName(),
                            lastMessageContent,
                            lastMessageDate,
                            offset);
                })
                .collect(Collectors.toList());

        return new ChatResponse.FindChatRoomListDTO(roomDTOS);
    }

    @Transactional
    public ChatResponse.FindMessageListInRoomDTO findMessageListInRoom(Long chatRoomId, Long userId, Integer startPage){
        // 권한 체크
        ChatUser chatUser = checkChatAuthority(userId, chatRoomId);

        List<ChatResponse.MessageDTD> messageDTOs = new ArrayList<>();
        boolean isLast = false;
        int currentPage = startPage;

        while(!isLast) {
            Pageable pageable = PageRequest.of(currentPage, 50, Sort.by("id"));
            Page<Message> messages = messageRepository.findByChatRoomId(chatRoomId, pageable);
            isLast = messages.isLast(); // 현재 페이지가 마지막 페이지인지 확인

            List<ChatResponse.MessageDTD> currentMessages = messages.getContent().stream()
                    .map(message -> new ChatResponse.MessageDTD(message.getId(),
                            message.getSenderName(),
                            message.getContent(),
                            message.getDate(),
                            message.getSenderId().equals(userId)))
                    .toList();

            messageDTOs.addAll(currentMessages); // 현재 페이지의 데이터를 추가
            currentPage++; // 다음 페이지로 이동
        }

        // 마지막으로 읽은 메시지의 id와 index 업데이트
        if (!messageDTOs.isEmpty()) {
            ChatResponse.MessageDTD lastMessageDTO = messageDTOs.get(messageDTOs.size() - 1);
            long chatNum = messageRepository.countByChatRoomId(chatRoomId);

            chatUser.updateLastMessage(lastMessageDTO.messageId(), chatNum - 1);
        }

        return new ChatResponse.FindMessageListInRoomDTO(chatUser.getLastMessageId(), messageDTOs);
    }

    @Transactional
    public void readMessage(ChatRequest.ReadMessageDTO requestDTO, Long userId){
        // 권한 체크
        ChatUser chatUser = checkChatAuthority(userId, requestDTO.chatRoomId());
        chatUser.updateLastMessage(requestDTO.messageId(), chatUser.getLastMessageIdx() + 1);
    }

    private Pageable createPageable(int page, int size, String sortProperty) {
        return PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, sortProperty));
    }

    private ChatUser checkChatAuthority(Long userId, Long chatRoomId){
        // 채팅방에 들어와있는지 여부 체크
        Optional<ChatUser> chatUserOP = chatUserRepository.findByUserIdAndChatRoomId(userId, chatRoomId);
        if(chatUserOP.isEmpty()){
            throw new CustomException(ExceptionCode.USER_FORBIDDEN);
        }

        return chatUserOP.get();
    }
}