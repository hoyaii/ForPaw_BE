package com.hong.ForPaw.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hong.ForPaw.controller.DTO.MessageRequest;
import com.hong.ForPaw.core.errors.CustomException;
import com.hong.ForPaw.core.errors.ExceptionCode;
import com.hong.ForPaw.domain.Chat.ChatRoom;
import com.hong.ForPaw.domain.Chat.Message;
import com.hong.ForPaw.domain.User.User;
import com.hong.ForPaw.repository.Chat.ChatRoomRepository;
import com.hong.ForPaw.repository.Chat.ChatUserRepository;
import com.hong.ForPaw.repository.Chat.MessageRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerEndpoint;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.RabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistry;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatService {

    private final RabbitListenerEndpointRegistry rabbitListenerEndpointRegistry;
    private final RabbitListenerContainerFactory<?> rabbitListenerContainerFactory;
    private final MessageRepository messageRepository;
    private final RabbitTemplate rabbitTemplate;
    private final SimpMessagingTemplate messagingTemplate;
    private final AmqpAdmin amqpAdmin;
    private final EntityManager entityManager;

    @Transactional
    public void sendMessage(Long chatRoomId, Long senderId, String senderName, String content){
        // 그룹의 채팅방
        ChatRoom chatRoomRef = entityManager.getReference(ChatRoom.class, chatRoomId);
        User userRef = entityManager.getReference(User.class, senderId);

        Message message = Message.builder()
                .sender(userRef)
                .chatRoom(chatRoomRef)
                .senderName(senderName)
                .content(content)
                .date(LocalDateTime.now())
                .build();

        messageRepository.save(message);

        // DTO를 통해 메시지 전송
        MessageRequest.MessageDTO messageDTO = new MessageRequest.MessageDTO(senderId, chatRoomId, senderName, content, message.getDate());
        String exchangeName = "chatroom." + chatRoomId + ".exchange";

        rabbitTemplate.convertAndSend(exchangeName, "", messageDTO);
    }

    public void registerExchange(Long chatRoomId){

        String exchangeName = "chatroom." + chatRoomId + ".exchange";
        FanoutExchange fanoutExchange = new FanoutExchange(exchangeName);
        amqpAdmin.declareExchange(fanoutExchange);
    }

    public void registerQueue(Long userId, Long chatRoomId){

        String queueName = "user.queue." + userId + ".chatroom." + chatRoomId;
        String exchangeName = "chatroom." + chatRoomId + ".exchange";

        Queue userQueue = new Queue(queueName, true);
        amqpAdmin.declareQueue(userQueue);

        // 해당 그룹 채팅방의 Fanout Exchange와 큐를 바인딩
        FanoutExchange fanoutExchange = new FanoutExchange(exchangeName);
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
            try {
                ObjectMapper objectMapper = new ObjectMapper();

                String messageBody = new String(message.getBody());
                MessageRequest.MessageDTO messageDTO = objectMapper.readValue(messageBody, MessageRequest.MessageDTO.class);

                sendStompMessage(messageDTO);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        rabbitListenerEndpointRegistry.registerListenerContainer(endpoint, rabbitListenerContainerFactory, true);
    }

    private void sendStompMessage(MessageRequest.MessageDTO messageDTO) {
        // 클라이언트가 구독하는 주제 경로
        String destination = "/topic/chatRoom." + messageDTO.chatRoomId();

        // 메시지를 해당 주제의 구독자들에게 전송
        messagingTemplate.convertAndSend(destination, messageDTO);
    }
}