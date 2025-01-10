package com.greenroom.server.api.domain.greenroom.entity;


import com.greenroom.server.api.domain.common.entity.BaseTime;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Table(name = "adornment")
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Adornment extends BaseTime {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long adornmentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id",nullable = false)
    private Item item;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "greenroom_id",nullable = false,updatable = false)
    private GreenRoom greenRoom;

    @Builder
    public Adornment (Item item, GreenRoom greenRoom){
        this.item = item;
        this.greenRoom = greenRoom;
    }

    public void updateItem(Item item){
        this.item = item;
    }
}

