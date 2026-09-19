package com.austin.module.account.mapper;

import com.austin.module.account.domain.UserAccount;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserAccountMapper extends BaseMapper<UserAccount> {

    @Select("SELECT * FROM user_account WHERE id = #{id} FOR UPDATE")
    UserAccount selectByIdForUpdate(@Param("id") long id);

    @Select("""
            SELECT id FROM user_account
            WHERE account_status = 'CANCEL_PENDING'
              AND cancel_requested_at <= #{cutoff}
            ORDER BY cancel_requested_at, id
            LIMIT #{limit}
            """)
    List<Long> selectCancellationDueIds(
            @Param("cutoff") LocalDateTime cutoff,
            @Param("limit") int limit);
}
