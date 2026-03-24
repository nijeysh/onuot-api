package com.onuot.api.domain.outfit.controller;

import com.onuot.api.domain.outfit.dto.OutfitResponse;
import com.onuot.api.domain.outfit.service.OutfitService;
import com.onuot.api.global.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/outfits")
@RequiredArgsConstructor
public class OutfitController {

    private final OutfitService outfitService;

    @GetMapping("/recommend")
    public ApiResponse<List<OutfitResponse>> recommend(@RequestParam double temperature) {
        return ApiResponse.ok(outfitService.recommendByTemperature(temperature));
    }
}
