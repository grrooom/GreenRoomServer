package com.greenroom.server.api.domain.user.entity;

import com.greenroom.server.api.domain.alram.entity.Alarm;
import com.greenroom.server.api.domain.common.BaseTime;
import com.greenroom.server.api.domain.greenroom.entity.Grade;
import com.greenroom.server.api.domain.user.dto.UserDto;
import com.greenroom.server.api.domain.user.enums.Provider;
import com.greenroom.server.api.domain.user.enums.Role;
import com.greenroom.server.api.domain.user.enums.UserStatus;
import com.greenroom.server.api.domain.user.enums.converter.ProviderConverter;
import com.greenroom.server.api.security.dto.SignupRequestDto;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Table(name = "users")
@Entity
@Getter
//@ToString
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseTime {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    private String name;

    private String email;

    private String password;

    private int totalSeed;

    private int weeklySeed;

    private String profileUrl;


    @Enumerated(EnumType.STRING)
    public Role role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grade_id")
    private Grade grade;

    @Enumerated(EnumType.STRING)
    @Convert(converter = ProviderConverter.class)
    private Provider provider;

    @Enumerated(EnumType.STRING)
    private UserStatus userStatus;

    @Builder
    public User(String name,String email,String password,String profileUrl,Grade grade,Role role,Provider provider) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.profileUrl = profileUrl;
        this.totalSeed = 0;
        this.weeklySeed = 0;
        this.grade = grade;
        this.role = role;
        this.provider = provider;
        this.userStatus = UserStatus.IN_ACTION;
    }

    public static User createUser(UserDto userDto, Grade grade){

        return User.builder()
                .name(userDto.getName())
                .email(userDto.getEmail())
                .grade(grade)
                .role(Role.GENERAL)
                .provider(userDto.getProvider())
                .build();
    }


    public static User createUser(SignupRequestDto signupRequestDto, Grade grade){

        User user =  User.builder()
                .email(signupRequestDto.getEmail())
                .grade(grade)
                .role(Role.GENERAL)
                .build();
        user.updatePassword(signupRequestDto.getPassword());
        return user;
    }

    public User updateUserName(String name){
        this.name = name;
        return this;
    }

    public User updateProfileUrl(String profileUrl){
        this.profileUrl = profileUrl;
        return this;
    }

    public void withdrawalUser(){
        this.userStatus = UserStatus.DELETE_PENDING;
    }

    public void updateTotalSeed(int plusSeed){
        this.totalSeed +=plusSeed;
    }
    public void updateWeeklySeed(int plusSeed){
        this.weeklySeed +=plusSeed;
    }
    public void updateGrade(Grade grade) {
        this.grade = grade;
    }
    public void updatePassword(String password){
        this.password= password;
    }

    public void updateProvider(Provider provider) {this.provider= provider;}
}
