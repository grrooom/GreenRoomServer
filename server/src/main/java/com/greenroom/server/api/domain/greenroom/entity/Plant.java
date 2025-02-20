package com.greenroom.server.api.domain.greenroom.entity;

import com.greenroom.server.api.domain.common.entity.BaseTime;
import jakarta.persistence.*;
import lombok.*;

@Table(name = "plant")
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Plant extends BaseTime {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long plantId;

    private String scientificName;

    private String commonName;

    private String plantPictureUrl;

    @Column(name="plant_picture_url_s3")
    private String plantPictureUrlS3;

    private Integer plantCount;

    private String waterCycle;

    private String lightDemand;

    private String growthTemperature;

    private String humidity;

    private String fertilizer;

    private String manageLevel;

    private String otherInformation;

    private String plantCategory;
    @Builder
    public Plant(String scientificName, String commonName, String plantPictureUrl, int plantCount, String waterCycle, String lightDemand, String growthTemperature, String humidity, String fertilizer, String manageLevel, String otherInformation,String plantCategory) {
        this.scientificName = scientificName;
        this.commonName = commonName;
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

    public void updateS3PlantPictureUrl(String url){
        this.plantPictureUrlS3 = url;
    }

}
