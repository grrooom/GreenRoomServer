package com.greenroom.server.api.domain.greenroom.entity;

import com.greenroom.server.api.domain.common.entity.BaseTime;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Table(name = "todo")
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Todo extends BaseTime {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long todoId;

    @Column(name = "base_date", columnDefinition = "DATE")
    private LocalDate baseDate;

    @Column(name = "next_todo_date", columnDefinition = "DATE")
    private LocalDate nextTodoDate;

    private Integer term;

    private Boolean useYn;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "greenroom_id")
    private GreenRoom greenRoom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "activity_id")
    private Activity activity;

    @Builder
    public Todo(LocalDate baseDate, Integer term,GreenRoom greenRoom, Activity activity, LocalDate nextTodoDate) {
        this.baseDate = baseDate;
        this.term = term;
        this.useYn = Boolean.TRUE;
        this.greenRoom = greenRoom;
        this.activity = activity;
        this.nextTodoDate = nextTodoDate;
    }

}
