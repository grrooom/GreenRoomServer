package com.greenroom.server.api.domain.greenroom.entity;

import com.greenroom.server.api.domain.common.entity.BaseTime;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Table(name = "todo")
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Todo extends BaseTime {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long todoId;

    private LocalDateTime firstStartDate;

    private LocalDateTime lastUpdateDate;

    private LocalDateTime nextTodoDate;

    private Integer duration;

    private Boolean useYn;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "greenroom_id")
    private GreenRoom greenRoom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "activity_id")
    private Activity activity;

    @Builder
    public Todo(LocalDateTime firstStartDate,LocalDateTime lastUpdateDate ,Integer duration, Boolean useYn, GreenRoom greenRoom, Activity activity, LocalDateTime nextTodoDate) {
        this.firstStartDate = firstStartDate;
        this.lastUpdateDate = lastUpdateDate;
        this.duration = duration;
        this.useYn = Boolean.TRUE;
        this.greenRoom = greenRoom;
        this.activity = activity;
        this.nextTodoDate = nextTodoDate;
    }

    public void updateLastUpdateDate(LocalDateTime lastUpdateDate){
        this.lastUpdateDate = lastUpdateDate;
    }
    public void updateNextTodoDate(LocalDateTime nextTodoDate){
        this.nextTodoDate = nextTodoDate;
    }
    public void updateUseYn(Boolean useYn){this.useYn = useYn;}
    public void updateDuration(Integer duration){this.duration=duration;}
}
