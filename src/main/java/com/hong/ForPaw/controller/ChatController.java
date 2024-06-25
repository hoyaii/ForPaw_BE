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
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
@RequestMapping("/api")
public class ChatController {

    private final ChatService chatService;

    // 테스트 시 MessageMapping => PostMapping으로 바꾸고, 응답이 없는 거 인지하고 있어야 함.
    @PostMapping("/chat/send")
    public ResponseEntity<?> sendMessage(@RequestBody ChatRequest.SendMessageDTO requestDTO, @AuthenticationPrincipal CustomUserDetails userDetails) {
        ChatResponse.SendMessageDTO responseDTO = chatService.sendMessage(requestDTO, userDetails.getUser().getId(), userDetails.getUsername());
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, responseDTO));
    }

    @GetMapping("/chatRooms")
    public ResponseEntity<?> findChatRoomList(@AuthenticationPrincipal CustomUserDetails userDetails){
        ChatResponse.FindChatRoomListDTO responseDTO = chatService.findChatRoomList(userDetails.getUser().getId());
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, responseDTO));
    }

    @GetMapping("/chatRooms/{chatRoomId}/messages")
    public ResponseEntity<?> findMessageListInRoom(@PathVariable Long chatRoomId, @RequestParam("page") Integer page, @AuthenticationPrincipal CustomUserDetails userDetails){
        ChatResponse.FindMessageListInRoomDTO responseDTO = chatService.findMessageListInRoom(chatRoomId, userDetails.getUser().getId(), page);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, responseDTO));
    }

    @GetMapping("/chatRooms/{chatRoomId}/drawer")
    public ResponseEntity<?> findChatRoomDrawer(@PathVariable Long chatRoomId, @AuthenticationPrincipal CustomUserDetails userDetails){
        ChatResponse.FindChatRoomDrawerDTO responseDTO = chatService.findChatRoomDrawer(chatRoomId, userDetails.getUser().getId());
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, responseDTO));
    }

    @GetMapping("/chatRooms/{chatRoomId}/images")
    public ResponseEntity<?> findChatRoomImages(@PathVariable Long chatRoomId, @RequestParam("page") Integer page, @AuthenticationPrincipal CustomUserDetails userDetails){
        ChatResponse.FindChatRoomImagesDTO responseDTO = chatService.findChatRoomImages(chatRoomId, userDetails.getUser().getId(), page);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, responseDTO));
    }

    // 테스트 시 MessageMapping => PostMapping으로 바꾸고, 응답이 없는 거 인지하고 있어야 함.
    @PostMapping("/chat/read")
    public void readMessage(@RequestBody ChatRequest.ReadMessageDTO requestDTO, @AuthenticationPrincipal CustomUserDetails userDetails) {
        chatService.readMessage(requestDTO, userDetails.getUser().getId());
    }
}
