package com.greenroom.server.api.domain.greenroom.entity;

import com.greenroom.server.api.domain.common.entity.BaseTime;
import com.greenroom.server.api.domain.greenroom.enums.GreenRoomStatus;
import com.greenroom.server.api.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Table(name = "greenroom")
@Entity
@Getter
@Builder(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GreenRoom extends BaseTime {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long greenroomId;

    private String name;

    private String pictureUrl;

    private String memo;

    @Enumerated(EnumType.STRING)
    private GreenRoomStatus greenroomStatus;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id",nullable = false, updatable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plant_id")
    private Plant plant;

    public static GreenRoom of(String name, String pictureUrl,User user, Plant plant){
        return GreenRoom.builder()
                .name(name)
                .pictureUrl(pictureUrl)
                .user(user)
                .memo(null)
                .greenroomStatus(GreenRoomStatus.ENABLED)
                .plant(plant)
                .build();
    }

    public void updateCreationDate(LocalDateTime date){this.createDate = date;}

    public void updateMemo(String memo){
        this.memo = memo;
    }

    public void updateName(String name){this.name = name;}

    public void updatePictureUrl(String pictureUrl) {this.pictureUrl = pictureUrl;}

    public void updateStatus (GreenRoomStatus greenRoomStatus){this.greenroomStatus = greenRoomStatus;}

    public void updatePlant(Plant plant){this.plant = plant;}
}
