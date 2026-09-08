package com.austin.module.profile.domain;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
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
@TableName("user_profile")
public class UserProfile {

    @TableId
    private Long accountId;

    private String nickname;

    private AvatarCode avatarCode;

    /**
     * 个人简介
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String bio;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String city;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String district;

    @Version
    private Integer version;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
