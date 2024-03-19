package com.hong.ForPaw.service;

import com.hong.ForPaw.controller.DTO.ChatRequest;
import com.hong.ForPaw.controller.DTO.ChatResponse;
import com.hong.ForPaw.core.errors.CustomException;
import com.hong.ForPaw.core.errors.ExceptionCode;
import com.hong.ForPaw.domain.Chat.ChatRoom;
import com.hong.ForPaw.domain.Chat.ChatUser;
import com.hong.ForPaw.domain.Chat.Message;
import com.hong.ForPaw.repository.Chat.ChatUserRepository;
import com.hong.ForPaw.repository.Chat.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerEndpoint;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.RabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatService {

    private final RabbitListenerEndpointRegistry rabbitListenerEndpointRegistry;
    private final RabbitListenerContainerFactory<?> rabbitListenerContainerFactory;
    private final MessageRepository messageRepository;
    private final ChatUserRepository chatUserRepository;
    private final RabbitTemplate rabbitTemplate;
    private final SimpMessagingTemplate messagingTemplate;
    private final AmqpAdmin amqpAdmin;

    @Transactional
    public void sendMessage(ChatRequest.SendMessageDTO requestDTO, Long senderId, String senderName){
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
        List<ChatUser> chatUsers = chatUserRepository.findByUserId(userId);

        List<ChatResponse.RoomDTO> roomDTOS = chatUsers.stream()
                .map(chatUser -> {
                    Message lastMessage = messageRepository.findFirstByChatRoomIdOrderByDateDesc(chatUser.getChatRoom().getId()).orElseThrow(
                            () -> new CustomException(ExceptionCode.MEETING_NOT_FOUND)
                    );
                    return new ChatResponse.RoomDTO(
                            chatUser.getChatRoom().getId(),
                            chatUser.getChatRoom().getName(),
                            lastMessage.getContent(),
                            lastMessage.getDate(),
                            chatUser.getOffset());
                })
                .collect(Collectors.toList());

        return new ChatResponse.FindChatRoomListDTO(roomDTOS);
    }

    @Transactional
    public ChatResponse.FindMessageListInRoomDTO findMessageListInRoom(Long chatRoomId, Long userId, Integer page){

        Pageable pageable = createPageable(page, 10);
        Page<Message> messages = messageRepository.findByChatRoomId(chatRoomId, pageable);

        List<ChatResponse.MessageDTD> messageDTDS = messages.getContent().stream()
                .map(message -> new ChatResponse.MessageDTD(message.getId(),
                        message.getSenderName(),
                        message.getContent(),
                        message.getDate(),
                        message.getSenderId().equals(userId)))
                .collect(Collectors.toList());

        return new ChatResponse.FindMessageListInRoomDTO(messageDTDS);
    }

    public void registerExchange(Long chatRoomId){

        String exchangeName = "chatroom." + chatRoomId + ".exchange";
        FanoutExchange fanoutExchange = new FanoutExchange(exchangeName);
        amqpAdmin.declareExchange(fanoutExchange);
    }

    public void registerQueue(Long userId, Long chatRoomId){

        String exchangeName = "chatroom." + chatRoomId + ".exchange";
        FanoutExchange fanoutExchange = new FanoutExchange(exchangeName);

        String queueName = "user.queue." + userId + ".chatroom." + chatRoomId;
        Queue userQueue = new Queue(queueName, true);
        amqpAdmin.declareQueue(userQueue);

        // 해당 그룹 채팅방의 Fanout Exchange와 큐를 바인딩
        Binding binding = BindingBuilder.bind(userQueue).to(fanoutExchange);
        amqpAdmin.declareBinding(binding);
    }

    public void registerListener(Long userId, Long chatRoomId) {
        // listenerId는 각 리스너의 고유 id
        String listenerId = "chatroom.listener." + userId + "." + chatRoomId;
        String queueName = "user.queue." + userId + ".chatroom." + chatRoomId;

        SimpleRabbitListenerEndpoint endpoint = new SimpleRabbitListenerEndpoint();
        endpoint.setId(listenerId);
        endpoint.setQueueNames(queueName);
        endpoint.setMessageListener(message -> {

        });

        rabbitListenerEndpointRegistry.registerListenerContainer(endpoint, rabbitListenerContainerFactory, true);
    }

    private Pageable createPageable(int page, int size) {
        return PageRequest.of(page, size);
    }
}