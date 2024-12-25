package com.hong.forapw.domain.inquiry;

import com.hong.forapw.domain.inquiry.model.InquiryResponse;
import com.hong.forapw.domain.inquiry.model.InquriyRequest;
import com.hong.forapw.security.CustomUserDetails;
import com.hong.forapw.common.utils.ApiUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class InquiryController {

    private final InquiryService inquiryService;

    @PostMapping("/supports")
    public ResponseEntity<?> submitInquiry(@RequestBody @Valid InquriyRequest.SubmitInquiry requestDTO, @AuthenticationPrincipal CustomUserDetails userDetails) {
        InquiryResponse.SubmitInquiryDTO responseDTO = inquiryService.submitInquiry(requestDTO, userDetails.getUser().getId());
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, responseDTO));
    }

    @PatchMapping("/supports/{inquiryId}")
    public ResponseEntity<?> updateInquiry(@RequestBody @Valid InquriyRequest.UpdateInquiry requestDTO, @PathVariable Long inquiryId, @AuthenticationPrincipal CustomUserDetails userDetails) {
        inquiryService.updateInquiry(requestDTO, inquiryId, userDetails.getUser().getId());
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, null));
    }

    @GetMapping("/supports")
    public ResponseEntity<?> findInquiryList(@AuthenticationPrincipal CustomUserDetails userDetails) {
        InquiryResponse.FindInquiryListDTO responseDTO = inquiryService.findInquiries(userDetails.getUser().getId());
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, responseDTO));
    }
}
