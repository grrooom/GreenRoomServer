package com.greenroom.server.api.domain.user.entity;

import com.greenroom.server.api.domain.common.entity.BaseTime;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;

@Table(name = "check_in")
@Entity
@Getter
@RequiredArgsConstructor
public class CheckIn extends BaseTime {

    @Id@GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long checkInId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    private LocalDate checkInDate;

    @Builder
    private CheckIn(User user){
        this.user = user;
    }

    public void updateCheckinDate(){
        this.checkInDate = LocalDate.now();
    }
    public static CheckIn createCheckIn(User user){
        return CheckIn.builder().user(user).build();

    }


}
