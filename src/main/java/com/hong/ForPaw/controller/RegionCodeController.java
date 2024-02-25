package com.hong.ForPaw.controller;

import com.hong.ForPaw.service.RegionCodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class RegionCodeController {

    private final RegionCodeService regionCodeService;

    @GetMapping("/regionCodes")
    public String loadRegionCode() throws IOException {
        regionCodeService.loadRegionCodeData();

        return "ho";
    }
}
