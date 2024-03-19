package com.hong.ForPaw.controller;

import com.hong.ForPaw.controller.DTO.ChatRequest;
import com.hong.ForPaw.controller.DTO.ChatResponse;
import com.hong.ForPaw.core.security.CustomUserDetails;
import com.hong.ForPaw.core.utils.ApiUtils;
import com.hong.ForPaw.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
@RequestMapping("/api")
public class ChatController {

    private final ChatService chatService;

    @MessageMapping("/chat.sendMessage")
    public void sendMessage(@Payload ChatRequest.SendMessageDTO requestDTO, @AuthenticationPrincipal CustomUserDetails userDetails) {

        chatService.sendMessage(requestDTO, userDetails.getUser().getId(), userDetails.getUsername());
    }

    @GetMapping("/chatRooms/{chatRoomId}/messages")
    public ResponseEntity<?> findMessageListInRoom(@PathVariable Long chatRoomId, @RequestParam("page") Integer page, @AuthenticationPrincipal CustomUserDetails userDetails){

        ChatResponse.FindMessageListInRoomDTO responseDTO = chatService.findMessageListInRoom(chatRoomId, userDetails.getUser().getId(), page);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, responseDTO));
    }
}
