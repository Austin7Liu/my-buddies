package com.austin.module.catalog.domain;

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
@TableName("topic_follow")
public class TopicFollow {

    @TableId
    private Long id;

    private Long topicId;

    private Long accountId;

    private LocalDateTime createdAt;
}
