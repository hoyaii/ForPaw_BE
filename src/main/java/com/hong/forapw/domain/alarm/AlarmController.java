package com.hong.forapw.domain.alarm;

import com.hong.forapw.domain.alarm.model.AlarmRequest;
import com.hong.forapw.domain.alarm.model.AlarmResponse;
import com.hong.forapw.security.CustomUserDetails;
import com.hong.forapw.common.utils.ApiUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Controller
@RequiredArgsConstructor
@RequestMapping("/api")
public class AlarmController {

    private final AlarmService alarmService;

    @GetMapping(value = "/alarms/connect", produces = "text/event-stream")
    public SseEmitter connectToAlarm(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return alarmService.connectToSseForAlarms(userDetails.getUser().getId().toString());
    }

    @GetMapping("/alarms")
    public ResponseEntity<?> findAlarmList(@AuthenticationPrincipal CustomUserDetails userDetails) {
        AlarmResponse.FindAlarmListDTO responseDTO = alarmService.findAlarms(userDetails.getUser().getId());
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, responseDTO));
    }

    @PostMapping("/alarms/read")
    public ResponseEntity<?> readAlarm(@RequestBody @Valid AlarmRequest.ReadAlarmDTO requestDTO, @AuthenticationPrincipal CustomUserDetails userDetails) {
        alarmService.updateAlarmAsRead(requestDTO.id(), userDetails.getUser().getId());
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, null));
    }
}