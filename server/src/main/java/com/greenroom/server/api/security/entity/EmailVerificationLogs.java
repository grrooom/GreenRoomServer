package com.greenroom.server.api.security.entity;

import com.greenroom.server.api.domain.common.BaseTime;
import com.greenroom.server.api.domain.user.enums.converter.ProviderConverter;
import com.greenroom.server.api.security.enums.VerificationStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
public class EmailVerificationLogs extends BaseTime {

    @Id@GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long emailLogId;
    private String email ;
    private int numberOfTrial;
    private String verificationToken ;

    @Enumerated(EnumType.STRING)
    private VerificationStatus verificationStatus;

    private LocalDateTime expires_at;

    public static EmailVerificationLogs createLog(String email,int trial,String token){
        return EmailVerificationLogs.builder()
                .email(email)
                .verificationToken(token)
                .numberOfTrial(trial)
                .expires_at(LocalDateTime.now().plusMinutes(15))
                .verificationStatus(VerificationStatus.PENDING)
                .build();
    }

    public void updateLog(String token){
        this.verificationToken = token;
        this.expires_at = LocalDateTime.now().plusMinutes(15);
        this.verificationStatus = VerificationStatus.PENDING;
        this.numberOfTrial = this.numberOfTrial>=5?1:this.numberOfTrial+1;
    }

    public void updateVerificationStatus(VerificationStatus status){
        this.verificationStatus = status;
    }


}
