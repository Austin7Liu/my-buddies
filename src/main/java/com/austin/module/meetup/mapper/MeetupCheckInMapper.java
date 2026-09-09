package com.austin.module.meetup.mapper;

import com.austin.module.meetup.domain.MeetupCheckIn;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MeetupCheckInMapper extends BaseMapper<MeetupCheckIn> {
}
