package com.austin.module.meetup.domain;

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
@TableName("meetup_check_in")
public class MeetupCheckIn {

    @TableId
    private Long id;

    private Long meetupId;

    private Long accountId;

    private Integer distanceMeters;

    private LocalDateTime checkedInAt;

    private LocalDateTime createdAt;
}
