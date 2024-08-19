package com.hong.ForPaw.controller;

import com.hong.ForPaw.controller.DTO.ChatRequest;
import com.hong.ForPaw.controller.DTO.ChatResponse;
import com.hong.ForPaw.core.security.CustomUserDetails;
import com.hong.ForPaw.core.utils.ApiUtils;
import com.hong.ForPaw.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/api")
public class ChatController {

    private final ChatService chatService;
    private static final String SORT_BY_DATE = "date";
    private static final String SORT_BY_ID = "id";

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
    public ResponseEntity<?> findMessageListInRoom(@PathVariable Long chatRoomId,
                                                   @PageableDefault(size = 50, sort = SORT_BY_DATE, direction = Sort.Direction.DESC) Pageable pageable, @AuthenticationPrincipal CustomUserDetails userDetails){
        ChatResponse.FindMessageListInRoomDTO responseDTO = chatService.findMessageListInRoom(chatRoomId, userDetails.getUser().getId(), pageable);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, responseDTO));
    }

    @GetMapping("/chatRooms/{chatRoomId}/drawer")
    public ResponseEntity<?> findChatRoomDrawer(@PathVariable Long chatRoomId, @AuthenticationPrincipal CustomUserDetails userDetails){
        ChatResponse.FindChatRoomDrawerDTO responseDTO = chatService.findChatRoomDrawer(chatRoomId, userDetails.getUser().getId());
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, responseDTO));
    }

    @GetMapping("/chatRooms/{chatRoomId}/images")
    public ResponseEntity<?> findChatRoomImageList(@PathVariable Long chatRoomId,
                                                @PageableDefault(size = 6, sort = SORT_BY_ID, direction = Sort.Direction.DESC) Pageable pageable, @AuthenticationPrincipal CustomUserDetails userDetails){
        List<ChatResponse.ImageObjectDTO> objectDTOS = chatService.findImageObjectList(chatRoomId, userDetails.getUser().getId(), pageable);
        ChatResponse.FindChatRoomImageList responseDTO = new ChatResponse.FindChatRoomImageList(objectDTOS);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, responseDTO));
    }

    @GetMapping("/chatRooms/{chatRoomId}/files")
    public ResponseEntity<?> findChatRoomFileList(@PathVariable Long chatRoomId,
                                                @PageableDefault(size = 6, sort = SORT_BY_ID, direction = Sort.Direction.DESC) Pageable pageable, @AuthenticationPrincipal CustomUserDetails userDetails){
        List<ChatResponse.FileObjectDTO> objectDTOS = chatService.findFileObjectList(chatRoomId, userDetails.getUser().getId(), pageable);
        ChatResponse.FindChatRoomFileListDTO responseDTO = new ChatResponse.FindChatRoomFileListDTO(objectDTOS);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, responseDTO));
    }

    @GetMapping("/chatRooms/{chatRoomId}/links")
    public ResponseEntity<?> findChatRoomLinkList(@PathVariable Long chatRoomId,
                                                  @PageableDefault(size = 6, sort = SORT_BY_ID, direction = Sort.Direction.DESC) Pageable pageable, @AuthenticationPrincipal CustomUserDetails userDetails){
        //List<ChatResponse.ObjectDTO> objectDTOS = chatService.findFileObjectList(chatRoomId, userDetails.getUser().getId(), pageable);
        ChatResponse.FindObjectListDTO responseDTO = new ChatResponse.FindObjectListDTO(objectDTOS);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, responseDTO));
    }

    @PostMapping("/chat/{chatId}/read")
    public ResponseEntity<?> readMessage(@PathVariable String chatId) {
        ChatResponse.ReadMessageDTO responseDTO = chatService.readMessage(chatId);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, responseDTO));
    }
}
