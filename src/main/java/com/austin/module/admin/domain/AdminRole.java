package com.austin.module.admin.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@TableName("admin_role")
public class AdminRole {

    @TableId
    private Long id;
    private AdminRoleCode code;
    private String displayName;
    private String description;
    private LocalDateTime createdAt;
}
