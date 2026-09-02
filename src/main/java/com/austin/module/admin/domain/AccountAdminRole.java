package com.austin.module.admin.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
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
@TableName("account_admin_role")
public class AccountAdminRole {

    @TableId
    private Long id;
    private Long accountId;
    private Long roleId;
    private Long grantedBy;
    private LocalDateTime grantedAt;
}
