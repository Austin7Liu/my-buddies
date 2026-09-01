package com.austin.module.identity.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("identity_verification")
public class IdentityVerification {

    @TableId
    private Long id;
    private Long accountId;
    private IdentityStatus status;
    /**
     * 不可逆 HMAC 实名主体指纹（身份证号）
     */
    private String subjectFingerprint;
    private LocalDate birthDate;
    private Gender gender;
    /**
     * 提供方标识
     */
    private String provider;
    /**
     * 提供方请求引用
     */
    private String providerReference;
    private String failureCode;
    private LocalDateTime submittedAt;
    private LocalDateTime verifiedAt;
    @Version
    private Integer version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

