package com.austin.module.circle.service;

import com.austin.module.circle.domain.Circle;
import com.austin.module.circle.domain.CircleStatus;
import com.baomidou.mybatisplus.core.metadata.IPage;

public interface CircleService {
    IPage<Circle> listPublic(long topicId, long page, long size);
    Circle getPublic(long circleId);
    IPage<Circle> listMine(long creatorId, long page, long size);
    IPage<Circle> listForReview(CircleStatus status, long page, long size);
    Circle create(long creatorId, long topicId, String name, String description, String city, String district);
    Circle update(long creatorId, long circleId, String name, String description, String city, String district);
    Circle approve(long reviewerId, long circleId);
    Circle reject(long reviewerId, long circleId, String reason);
    Circle setEnabled(long reviewerId, long circleId, boolean enabled);
}

