package com.onuot.api.modules.outfit.application;

import com.onuot.api.modules.outfit.application.dto.OutfitResponse;
import com.onuot.api.modules.outfit.domain.OutfitRecommendationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OutfitService {

    private final OutfitRecommendationRepository outfitRecommendationRepository;

    public List<OutfitResponse> recommendByTemperature(double temperature) {
        return outfitRecommendationRepository.findByTemperature(temperature).stream()
                .map(OutfitResponse::from)
                .toList();
    }
}
