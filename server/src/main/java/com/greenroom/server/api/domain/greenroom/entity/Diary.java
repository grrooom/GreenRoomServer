package com.greenroom.server.api.domain.greenroom.entity;

import com.greenroom.server.api.domain.common.BaseTime;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Table(name = "diary")
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Diary extends BaseTime {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long diaryId;

    private String diaryPictureUrl;

    private String title;

    private String content;

    @Builder
    public Diary(String diaryPictureUrl, String title, String content, GreenRoom greenRoom) {
        this.diaryPictureUrl = diaryPictureUrl;
        this.title = title;
        this.content = content;
        this.greenRoom = greenRoom;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "greenroom_id")
    private GreenRoom greenRoom;
}
