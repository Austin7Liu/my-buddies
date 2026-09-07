package com.austin.module.meetup.mapper;

import com.austin.module.meetup.domain.MeetupAuditLog;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MeetupAuditLogMapper extends BaseMapper<MeetupAuditLog> {
}
