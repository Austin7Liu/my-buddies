package com.austin.module.admin.mapper;

import com.austin.module.admin.domain.AccountAdminRole;
import com.austin.module.admin.domain.AdminRoleCode;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AccountAdminRoleMapper extends BaseMapper<AccountAdminRole> {

    @Select("""
            SELECT r.code
            FROM account_admin_role ar
            JOIN admin_role r ON r.id = ar.role_id
            WHERE ar.account_id = #{accountId}
            ORDER BY r.id
            """)
    List<AdminRoleCode> selectRoleCodes(@Param("accountId") long accountId);
}
