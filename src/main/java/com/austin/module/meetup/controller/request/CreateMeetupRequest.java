package com.austin.module.meetup.controller.request;

import com.austin.module.meetup.domain.GenderRequirement;
import com.austin.module.meetup.domain.MeetupMode;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public record CreateMeetupRequest(
        @Positive(message = "话题 ID 必须为正数")
        Long topicId,
        @Positive(message = "圈子 ID 必须为正数")
        Long circleId,
        @NotNull(message = "活动模式不能为空")
        MeetupMode meetupMode,
        @NotBlank(message = "活动标题不能为空")
        @Size(max = 100, message = "活动标题不能超过 100 个字符")
        String title,
        @NotBlank(message = "活动说明不能为空")
        @Size(max = 2000, message = "活动说明不能超过 2000 个字符")
        String description,
        @NotNull(message = "开始时间不能为空")
        LocalDateTime startTime,
        @NotNull(message = "结束时间不能为空")
        LocalDateTime endTime,
        @NotNull(message = "报名截止时间不能为空")
        LocalDateTime applicationDeadline,
        @Size(max = 64, message = "城市不能超过 64 个字符")
        String city,
        @Size(max = 64, message = "区域不能超过 64 个字符")
        String district,
        @Size(max = 128, message = "地点名称不能超过 128 个字符")
        String locationName,
        @Size(max = 255, message = "详细地址不能超过 255 个字符")
        String address,
        @NotNull(message = "活动人数不能为空")
        @Min(value = 2, message = "活动人数至少为 2 人")
        @Max(value = 100, message = "活动人数不能超过 100 人")
        Integer capacity,
        @NotNull(message = "最低年龄不能为空")
        @Min(value = 18, message = "最低年龄不能小于 18 岁")
        Integer minimumAge,
        @NotNull(message = "最高年龄不能为空")
        @Min(value = 18, message = "最高年龄不能小于 18 岁")
        Integer maximumAge,
        @NotNull(message = "性别要求不能为空")
        GenderRequirement genderRequirement,
        @Size(max = 255, message = "技能要求不能超过 255 个字符")
        String skillRequirement) {
}
