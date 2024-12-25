package com.hong.forapw.integration.s3;

import com.amazonaws.HttpMethod;
import com.hong.forapw.security.CustomUserDetails;
import com.hong.forapw.common.utils.ApiUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.net.URL;

@Controller
@RequiredArgsConstructor
@RequestMapping("/api")
public class S3Controller {

    private final S3Service s3Service;

    @GetMapping("/aws/presigned-url")
    public ResponseEntity<?> generatePresignedUrl(@AuthenticationPrincipal CustomUserDetails userDetails) {
        URL url = s3Service.generatePresignedUrl(userDetails.getUser().getId(), HttpMethod.PUT);
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, url.toString()));
    }
}
