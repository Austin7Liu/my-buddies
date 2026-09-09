package com.austin.module.notification.mapper;

import com.austin.module.notification.domain.UserNotification;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserNotificationMapper extends BaseMapper<UserNotification> {
}
