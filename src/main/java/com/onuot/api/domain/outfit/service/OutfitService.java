package com.onuot.api.domain.outfit.service;

import com.onuot.api.domain.outfit.dto.OutfitResponse;
import com.onuot.api.domain.outfit.repository.OutfitRecommendationRepository;
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
