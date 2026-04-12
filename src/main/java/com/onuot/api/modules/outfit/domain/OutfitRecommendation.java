package com.onuot.api.modules.outfit.domain;

import com.onuot.api.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "outfit_recommendations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OutfitRecommendation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private double minTemperature;

    @Column(nullable = false)
    private double maxTemperature;

    @Column(nullable = false)
    private String category;

    @Column(nullable = false)
    private String description;

    @Builder
    public OutfitRecommendation(double minTemperature, double maxTemperature,
                                String category, String description) {
        this.minTemperature = minTemperature;
        this.maxTemperature = maxTemperature;
        this.category = category;
        this.description = description;
    }
}
