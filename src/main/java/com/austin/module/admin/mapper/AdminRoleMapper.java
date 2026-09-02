package com.austin.module.admin.mapper;

import com.austin.module.admin.domain.AdminRole;
import com.austin.module.admin.domain.AdminRoleCode;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AdminRoleMapper extends BaseMapper<AdminRole> {

    @Select("SELECT * FROM admin_role WHERE code = #{code} FOR UPDATE")
    AdminRole lockByCode(@Param("code") AdminRoleCode code);
}
