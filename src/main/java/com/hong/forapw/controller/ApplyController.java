package com.hong.forapw.controller;

import com.hong.forapw.controller.dto.ApplyRequest;
import com.hong.forapw.controller.dto.ApplyResponse;
import com.hong.forapw.core.security.CustomUserDetails;
import com.hong.forapw.core.utils.ApiUtils;
import com.hong.forapw.service.ApplyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
@RequestMapping("/api")
public class ApplyController {

    private final ApplyService applyService;

    @PostMapping("/animals/{animalId}/apply")
    public ResponseEntity<?> applyAdoption(@RequestBody @Valid ApplyRequest.ApplyAdoptionDTO requestDTO, @PathVariable Long animalId, @AuthenticationPrincipal CustomUserDetails userDetails) {
        ApplyResponse.CreateApplyDTO responseDTO = applyService.applyAdoption(requestDTO, userDetails.getUser().getId(), animalId);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, responseDTO));
    }

    @GetMapping("/applies")
    public ResponseEntity<?> findApplyList(@AuthenticationPrincipal CustomUserDetails userDetails) {
        ApplyResponse.FindApplyListDTO responseDTO = applyService.findApplyList(userDetails.getUser().getId());
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, responseDTO));
    }

    @PatchMapping("/applies/{applyId}")
    public ResponseEntity<?> updateApply(@RequestBody @Valid ApplyRequest.UpdateApplyDTO requestDTO, @PathVariable Long applyId, @AuthenticationPrincipal CustomUserDetails userDetails) {
        applyService.updateApply(requestDTO, applyId, userDetails.getUser().getId());
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, null));
    }

    // 권한 처리가 필요함
    @DeleteMapping("/applies/{applyId}")
    public ResponseEntity<?> deleteApply(@PathVariable Long applyId, @AuthenticationPrincipal CustomUserDetails userDetails) {
        applyService.deleteApply(applyId, userDetails.getUser().getId());
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, null));
    }
}
