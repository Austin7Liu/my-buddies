package com.austin.module.meetup.mapper;

import com.austin.module.meetup.domain.Meetup;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.time.LocalDateTime;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface MeetupMapper extends BaseMapper<Meetup> {

    @Select("SELECT * FROM meetup WHERE id = #{id} FOR UPDATE")
    Meetup selectByIdForUpdate(@Param("id") long id);

    @Select("""
            SELECT CASE WHEN COUNT(*) > 0 THEN TRUE ELSE FALSE END
            FROM meetup m
            JOIN meetup_participant mp ON mp.meetup_id = m.id
            WHERE mp.account_id = #{accountId}
              AND mp.status = 'ACCEPTED'
              AND m.status IN ('OPEN', 'CONFIRMED')
              AND m.id <> #{excludedMeetupId}
              AND m.start_time < #{endTime}
              AND m.end_time > #{startTime}
            """)
    boolean hasAcceptedTimeConflict(
            @Param("accountId") long accountId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("excludedMeetupId") long excludedMeetupId);
}
