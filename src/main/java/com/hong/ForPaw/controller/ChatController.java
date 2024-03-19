package com.hong.ForPaw.controller;

import com.hong.ForPaw.controller.DTO.MessageRequest;
import com.hong.ForPaw.core.security.CustomUserDetails;
import com.hong.ForPaw.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.LocalDateTime;

@Controller
@RequiredArgsConstructor
@RequestMapping("/api")
public class ChatController {

    private final ChatService chatService;

    @MessageMapping("/chat.sendMessage")
    public void sendMessage(@Payload MessageRequest.SendMessageDTO requestDTO, @AuthenticationPrincipal CustomUserDetails userDetails) {

        chatService.sendMessage(requestDTO, userDetails.getUser().getId(), userDetails.getUsername());
    }
}
