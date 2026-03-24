package com.onuot.api.domain.outfit.dto;

import com.onuot.api.domain.outfit.entity.OutfitRecommendation;
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
