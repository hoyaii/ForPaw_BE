package com.hong.ForPaw.service;

import com.hong.ForPaw.controller.DTO.ChatRequest;
import com.hong.ForPaw.controller.DTO.ChatResponse;
import com.hong.ForPaw.core.errors.CustomException;
import com.hong.ForPaw.core.errors.ExceptionCode;
import com.hong.ForPaw.domain.Chat.ChatUser;
import com.hong.ForPaw.domain.Chat.Message;
import com.hong.ForPaw.repository.Chat.ChatImageRepository;
import com.hong.ForPaw.repository.Chat.ChatRoomRepository;
import com.hong.ForPaw.repository.Chat.ChatUserRepository;
import com.hong.ForPaw.repository.Chat.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatService {

    private final MessageRepository messageRepository;
    private final ChatUserRepository chatUserRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatImageRepository chatImageRepository;
    private final BrokerService brokerService;
    private final SimpMessagingTemplate messagingTemplate;
    private static final String SORT_BY_ID = "id";

    @Transactional
    public ChatResponse.SendMessageDTO sendMessage(ChatRequest.SendMessageDTO requestDTO, Long senderId, String senderName){
        // 권한 체크
        checkChatAuthority(senderId, requestDTO.chatRoomId());

        // 전송을 위한 메시지 DTO
        String messageId = UUID.randomUUID().toString();
        LocalDateTime sendDate = LocalDateTime.now();

        ChatRequest.MessageDTO messageDTO = new ChatRequest.MessageDTO(
                messageId,
                requestDTO.chatRoomId(),
                senderId,
                senderName,
                requestDTO.content(),
                requestDTO.images(),
                sendDate
        );

        // 메시지 브로커에 전송 (알람과 이미지는 비동기로 처리)
        brokerService.produceChatToRoom(requestDTO.chatRoomId(), messageDTO);

        // STOMP 프로토콜을 통한 실시간 메시지 전송
        String destination = "/room/" + requestDTO.chatRoomId();
        messagingTemplate.convertAndSend(destination, messageDTO);

        return new ChatResponse.SendMessageDTO(messageId);
    }

    @Transactional
    public ChatResponse.FindChatRoomListDTO findChatRoomList(Long userId){
        // chatRoom을 패치조인
        List<ChatUser> chatUsers = chatUserRepository.findByUserIdWithChatRoom(userId);

        List<ChatResponse.RoomDTO> roomDTOS = chatUsers.stream()
                .map(chatUser -> {
                    // 마지막으로 읽은 메시지
                    String lastMessageId = chatUser.getLastMessageId();
                    String lastMessageContent = null;
                    LocalDateTime lastMessageDate = null;

                    // 마지막으로 읽은 페이지 (인덱스/50)
                    Long lastReadMessageIdx = chatUser.getLastMessageIdx();
                    Long offset = 0L;

                    // 채팅방에서 메시지를 읽은 적이 있다면
                    if (lastMessageId != null) {
                        Optional<Message> lastMessageOP = messageRepository.findById(lastMessageId);
                        lastMessageContent = lastMessageOP.map(Message::getContent).orElse(null);
                        lastMessageDate = lastMessageOP.map(Message::getDate).orElse(null);
                        offset = lastReadMessageIdx != 0L ? lastReadMessageIdx / 50 : 0L;
                    }

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

        List<ChatResponse.MessageDTD> messageDTOS = new ArrayList<>();
        boolean isLast = false;
        int currentPage = startPage;

        while(!isLast) {
            Pageable pageable = createAscSortedPageable(currentPage, 50, SORT_BY_ID);
            Page<Message> messages = messageRepository.findByChatRoomId(chatRoomId, pageable);

            // 현재 페이지가 마지막 페이지인지 확인
            isLast = messages.isLast();

            List<ChatResponse.MessageDTD> currentMessageDTOS = messages.getContent().stream()
                    .map(message -> {
                        List<ChatResponse.ChatImageDTO> chatImageDTOS = message.getImageURLs().stream()
                                .map(ChatResponse.ChatImageDTO::new)
                                .toList();

                       return new ChatResponse.MessageDTD(
                               message.getId(),
                               message.getSenderName(),
                               message.getContent(),
                               chatImageDTOS,
                               message.getDate(),
                               message.getSenderId().equals(userId));
                    })
                    .toList();

            messageDTOS.addAll(currentMessageDTOS); // 현재 페이지의 데이터를 추가
            currentPage++; // 다음 페이지로 이동
        }

        // 마지막으로 읽은 메시지의 id와 index 업데이트
        if (!messageDTOS.isEmpty()) {
            ChatResponse.MessageDTD lastMessage = messageDTOS.get(messageDTOS.size() - 1);
            long chatNum = messageRepository.countByChatRoomId(chatRoomId);

            chatUser.updateLastMessage(lastMessage.messageId(), chatNum - 1);
        }

        return new ChatResponse.FindMessageListInRoomDTO(chatUser.getLastMessageId(), messageDTOS);
    }

    @Transactional
    public ChatResponse.FindChatRoomDrawerDTO findChatRoomDrawer(Long chatRoomId, Long userId){
        // 권한 체크
        checkChatAuthority(userId, chatRoomId);

        // 채팅방에 참여한 유저
        List<ChatResponse.ChatUserDTO> chatUserDTOS = chatRoomRepository.findAllUserByChatRoomId(chatRoomId).stream()
                .map(user -> new ChatResponse.ChatUserDTO(user.getId(), user.getName()))
                .toList();

        // 채팅방의 이미지
        Pageable pageable = createDescSortedPageable(0, 6, SORT_BY_ID);
        List<ChatResponse.ChatImageDTO> chatImageDTOS = chatImageRepository.findByChatRoomId(chatRoomId, pageable).getContent().stream()
                .map(chatImage -> new ChatResponse.ChatImageDTO(chatImage.getImageURL()))
                .toList();

        return new ChatResponse.FindChatRoomDrawerDTO(chatImageDTOS, chatUserDTOS);
    }

    @Transactional
    public ChatResponse.FindChatRoomImagesDTO findChatRoomImages(Long chatRoomId, Long userId, Integer page){
        // 권한 체크
        checkChatAuthority(userId, chatRoomId);

        // 채팅방의 이미지
        Pageable pageable = createDescSortedPageable(page, 6, SORT_BY_ID);
        List<ChatResponse.ChatImageDTO> chatImageDTOS = chatImageRepository.findByChatRoomId(chatRoomId, pageable).getContent().stream()
                .map(chatImage -> new ChatResponse.ChatImageDTO(chatImage.getImageURL()))
                .toList();

        return new ChatResponse.FindChatRoomImagesDTO(chatImageDTOS);
    }

    @Transactional
    public void readMessage(ChatRequest.ReadMessageDTO requestDTO, Long userId){
        // 권한 체크
        ChatUser chatUser = checkChatAuthority(userId, requestDTO.chatRoomId());

        chatUser.updateLastMessage(requestDTO.messageId(), chatUser.getLastMessageIdx() + 1);
    }

    private ChatUser checkChatAuthority(Long userId, Long chatRoomId){
        // 채팅방에 들어와있는지 여부 체크
        Optional<ChatUser> chatUserOP = chatUserRepository.findByUserIdAndChatRoomId(userId, chatRoomId);
        if(chatUserOP.isEmpty()){
            throw new CustomException(ExceptionCode.USER_FORBIDDEN);
        }

        return chatUserOP.get();
    }

    private Pageable createDescSortedPageable(int page, int size, String sortProperty) {
        return PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, sortProperty));
    }

    private Pageable createAscSortedPageable(int page, int size, String sortProperty) {
        return PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, sortProperty));
    }
}