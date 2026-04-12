package com.onuot.api.modules.outfit.application.dto;

import com.onuot.api.modules.outfit.domain.OutfitRecommendation;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class OutfitResponse {

    private Long id;
    private String category;
    private String description;

    public static OutfitResponse from(OutfitRecommendation recommendation) {
        return new OutfitResponse(
                recommendation.getId(),
                recommendation.getCategory(),
                recommendation.getDescription()
        );
    }
}
