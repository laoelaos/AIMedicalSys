package com.aimedical.modules.admin.entity;

import com.aimedical.common.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "sys_token")
@Data
public class TokenStore extends BaseEntity {

    private Long userId;

    @Column(unique = true, nullable = false, length = 2048)
    private String token;

    @Column(length = 2048)
    private String refreshToken;

    @Column(length = 20)
    private String tokenType;

    private LocalDateTime expiresAt;

}
