package com.greenroom.server.api.domain.greenroom.entity;

import com.greenroom.server.api.domain.common.entity.BaseTime;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Table(name = "diary")
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Diary extends BaseTime {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long diaryId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "greenroom_id")
    private GreenRoom greenRoom;

    private String diaryPictureUrl;

    private String title;

    private String content;

    private LocalDate date;

    @Builder
    public Diary(String diaryPictureUrl, String title, String content, GreenRoom greenRoom, LocalDate date) {
        this.diaryPictureUrl = diaryPictureUrl;
        this.title = title;
        this.content = content;
        this.greenRoom = greenRoom;
        this.date = date;
    }

    public static Diary createDiary(String title, String content, GreenRoom greenRoom, String diaryPictureUrl,LocalDate date){
        return Diary.builder().title(title).content(content).greenRoom(greenRoom).diaryPictureUrl(diaryPictureUrl).date(date).build();
    }

    public void updateCreateDate(LocalDateTime createDate){
        this.createDate = createDate;
    }

}
