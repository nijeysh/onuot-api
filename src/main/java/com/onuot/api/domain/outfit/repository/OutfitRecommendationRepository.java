package com.onuot.api.domain.outfit.repository;

import com.onuot.api.domain.outfit.entity.OutfitRecommendation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OutfitRecommendationRepository extends JpaRepository<OutfitRecommendation, Long> {

    @Query("SELECT o FROM OutfitRecommendation o WHERE o.minTemperature <= :temp AND o.maxTemperature >= :temp")
    List<OutfitRecommendation> findByTemperature(@Param("temp") double temperature);
}
