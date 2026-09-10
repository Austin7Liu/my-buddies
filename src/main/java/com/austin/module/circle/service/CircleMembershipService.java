package com.austin.module.circle.service;

import com.austin.module.circle.domain.Circle;
import com.austin.module.circle.domain.CircleMember;
import com.baomidou.mybatisplus.core.metadata.IPage;
import java.time.LocalDateTime;

public interface CircleMembershipService {

    CircleMember join(long accountId, long circleId);

    CircleMember leave(long accountId, long circleId);

    CircleMember getMine(long accountId, long circleId);

    IPage<CircleMember> listMembers(long circleId, long page, long size);

    IPage<Circle> listMyCircles(long accountId, long page, long size);

    void ensureActiveMember(long accountId, long circleId);

    void createOwnerMembership(long accountId, long circleId, LocalDateTime now);
}
