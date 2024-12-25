package com.hong.forapw.domain.region;

import com.hong.forapw.common.utils.ApiUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class RegionCodeController {

    private final RegionCodeService regionCodeService;

    @GetMapping("/regionCodes/import")
    public ResponseEntity<?> loadRegionCode() {
        regionCodeService.fetchRegionCode();
        return ResponseEntity.ok().body(ApiUtils.success(HttpStatus.OK, null));
    }
}