package com.austin.module.account.mapper;

import com.austin.module.account.domain.UserAccount;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserAccountMapper extends BaseMapper<UserAccount> {

    @Select("SELECT * FROM user_account WHERE id = #{id} FOR UPDATE")
    UserAccount selectByIdForUpdate(@Param("id") long id);
}
