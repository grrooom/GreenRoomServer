package com.greenroom.server.api.domain.greenroom.entity;

import com.greenroom.server.api.domain.common.entity.BaseTime;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Table(name = "todo_log")
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TodoLog extends BaseTime {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long todoLogId;

    @ManyToOne
    @JoinColumn(name = "greenroom_id")
    private GreenRoom greenRoom;

    @ManyToOne
    @JoinColumn(name = "activity_id")
    private Activity activity ;


    @Builder
    public TodoLog(GreenRoom greenRoom,Activity activity) {
        this.greenRoom = greenRoom;
        this.activity = activity;
    }
}

