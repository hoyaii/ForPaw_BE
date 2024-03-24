package com.hong.ForPaw.service;

import com.hong.ForPaw.controller.DTO.ChatRequest;
import com.hong.ForPaw.domain.Alarm.Alarm;
import com.hong.ForPaw.domain.Chat.Message;
import com.hong.ForPaw.repository.Chat.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerEndpoint;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.RabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistry;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BrokerService {

    private final MessageRepository messageRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final RabbitListenerEndpointRegistry rabbitListenerEndpointRegistry;
    private final RabbitListenerContainerFactory<?> rabbitListenerContainerFactory;
    private final RabbitTemplate rabbitTemplate;
    private final AmqpAdmin amqpAdmin;
    private final AlarmService alarmService;
    private final MessageConverter converter;

    public void registerDirectExchange(String exchangeName){
        DirectExchange fanoutExchange = new DirectExchange(exchangeName);
        amqpAdmin.declareExchange(fanoutExchange);
    }

    public void registerChatQueue(String exchangeName, String queueName){
        DirectExchange directExchange = new DirectExchange(exchangeName);

        Queue queue = new Queue(queueName, true);
        amqpAdmin.declareQueue(queue);

        // 해당 그룹 채팅방의 Fanout Exchange와 큐를 바인딩
        Binding binding = BindingBuilder.bind(queue).to(directExchange).with(queueName);
        amqpAdmin.declareBinding(binding);
    }

    public void registerChatListener(String listenerId, String queueName) {
        SimpleRabbitListenerEndpoint endpoint = new SimpleRabbitListenerEndpoint();
        endpoint.setId(listenerId);
        endpoint.setQueueNames(queueName);
        endpoint.setMessageListener(m -> {
            ChatRequest.MessageDTO messageDTO = (ChatRequest.MessageDTO) converter.fromMessage(m);

            // 메시지 저장
            Message message = Message.builder()
                    .chatRoomId(messageDTO.chatRoomId())
                    .senderId(messageDTO.senderId())
                    .senderName(messageDTO.senderName())
                    .content(messageDTO.content())
                    .date(messageDTO.date())
                    .build();
            messageRepository.save(message);
        });

        rabbitListenerEndpointRegistry.registerListenerContainer(endpoint, rabbitListenerContainerFactory, true);
    }

    public void produceAlarm(String routingKey, Alarm alarm) {
        rabbitTemplate.convertAndSend("alarm.exchange", routingKey, alarm);
    }

    @RabbitListener(queues = "alarm.queue")
    public void consumeAlarm(Alarm alarm) {
        alarmService.send(alarm);
    }
}
