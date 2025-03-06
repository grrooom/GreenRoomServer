package com.greenroom.server.api.domain.greenroom.entity;

import com.greenroom.server.api.domain.common.entity.BaseTime;
import com.greenroom.server.api.domain.greenroom.enums.ItemDetailType;
import com.greenroom.server.api.domain.greenroom.enums.ItemType;
import com.greenroom.server.api.domain.user.entity.Grade;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Table(name = "item")
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Item extends BaseTime {

    @Id @GeneratedValue(strategy =  GenerationType.IDENTITY)
    private Long itemId;

    @Enumerated(EnumType.STRING)
    private ItemType itemType;

    @Enumerated(EnumType.STRING)
    private ItemDetailType itemDetailType;

    private String itemName;

    private String imageUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grade_id")
    private Grade grade;

    @Builder
    public Item(ItemType itemType, ItemDetailType itemDetailType, String itemName, String imageUrl, Grade grade) {
        this.itemType = itemType;
        this.itemDetailType = itemDetailType;
        this.itemName = itemName;
        this.imageUrl = imageUrl;
        this.grade = grade;
    }
}
