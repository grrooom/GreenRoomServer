package com.greenroom.server.api.domain.greenroom.entity;

import com.greenroom.server.api.domain.common.entity.BaseTime;
import com.greenroom.server.api.domain.greenroom.enums.GreenRoomStatus;
import com.greenroom.server.api.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Table(name = "greenroom")
@Entity
@Getter
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

    @Builder
    public GreenRoom(String name, String pictureUrl,User user, Plant plant) {
        this.name = name;
        this.pictureUrl = pictureUrl;
        this.greenroomStatus = GreenRoomStatus.ENABLED;
        this.user = user;
        this.plant = plant;
    }

    public void updateMemo(String memo){
        this.memo = memo;
    }

    public void updateName(String name){this.name = name;}

    public void updatePictureUrl(String pictureUrl) {this.pictureUrl = pictureUrl;}

    public void updateStatus (GreenRoomStatus greenRoomStatus){this.greenroomStatus = greenRoomStatus;}

    public void updatePlant(Plant plant){this.plant = plant;}
}
