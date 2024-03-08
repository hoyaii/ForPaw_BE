package com.hong.ForPaw.controller;

import com.hong.ForPaw.core.security.CustomUserDetails;
import com.hong.ForPaw.core.utils.ApiUtils;
import com.hong.ForPaw.service.AlarmService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Controller
@RequiredArgsConstructor
@RequestMapping("/api")
public class AlarmController {

    private final AlarmService alarmService;

    @GetMapping(value = "/alarm/connect", produces = "text/event-stream")
    public ResponseEntity<?> connectToAlarm(@RequestHeader(value = "Last-Event-ID", required = false, defaultValue = "") String lastEventId, @AuthenticationPrincipal CustomUserDetails userDetails){

        SseEmitter sseEmitter= alarmService.connectToAlarm(userDetails.getUser().getId().toString(), lastEventId);
        return ResponseEntity.ok(ApiUtils.success(HttpStatus.OK, sseEmitter));
    }
}