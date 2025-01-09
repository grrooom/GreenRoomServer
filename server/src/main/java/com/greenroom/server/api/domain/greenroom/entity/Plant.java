package com.greenroom.server.api.domain.greenroom.entity;

import com.greenroom.server.api.domain.common.BaseTime;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Table(name = "plant")
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Plant extends BaseTime {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long plantId;

    private String plantAlias;

    private String distributionName;

    private String plantPictureUrl;

    private int plantCount;

    private String waterCycle;

    private String lightDemand;

    private String growthTemperature;

    private String humidity;

    private String fertilizer;

    private String manageLevel;

    private String otherInformation;

    private String plantCategory;
    @Builder
    public Plant(String plantAlias, String distributionName, String plantPictureUrl, int plantCount, String waterCycle, String lightDemand, String growthTemperature, String humidity, String fertilizer, String manageLevel, String otherInformation,String plantCategory) {
        this.plantAlias = plantAlias;
        this.distributionName = distributionName;
        this.plantPictureUrl = plantPictureUrl;
        this.plantCount = plantCount;
        this.waterCycle = waterCycle;
        this.lightDemand = lightDemand;
        this.growthTemperature = growthTemperature;
        this.humidity = humidity;
        this.fertilizer = fertilizer;
        this.manageLevel = manageLevel;
        this.otherInformation = otherInformation;
        this.plantCategory = plantCategory;
    }

    public void updatePlantCount(){
        plantCount +=1;
    }

}
