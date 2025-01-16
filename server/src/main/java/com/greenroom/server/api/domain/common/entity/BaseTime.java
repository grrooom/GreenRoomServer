package com.greenroom.server.api.domain.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * entity 공통 컬럼 클래스
 */
@MappedSuperclass
@Getter
public class BaseTime {

    @Column(updatable = false,insertable = false)
    protected LocalDateTime createDate;

    @Column(insertable = false,updatable = false)
    protected LocalDateTime updateDate;

}
