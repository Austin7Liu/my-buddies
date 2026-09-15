package com.austin.module.search.service;

import com.austin.module.circle.domain.Circle;
import com.austin.module.circle.mapper.CircleMapper;
import com.austin.module.meetup.domain.Meetup;
import com.austin.module.meetup.mapper.MeetupMapper;
import com.austin.module.post.domain.Post;
import com.austin.module.post.mapper.PostMapper;
import com.austin.module.search.domain.SearchDocumentType;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SearchCascadeService {

    private final CircleMapper circleMapper;
    private final PostMapper postMapper;
    private final MeetupMapper meetupMapper;
    private final SearchOutboxService outboxService;

    @Transactional
    public void expand(SearchDocumentType type, long aggregateId) {
        outboxService.recordRefresh(type, aggregateId);
        if (type == SearchDocumentType.TOPIC) {
            circleMapper.selectList(new LambdaQueryWrapper<Circle>().eq(Circle::getTopicId, aggregateId))
                    .forEach(circle -> outboxService.recordCascade(SearchDocumentType.CIRCLE, circle.getId()));
            postMapper.selectList(new LambdaQueryWrapper<Post>().eq(Post::getTopicId, aggregateId)
                            .isNull(Post::getCircleId))
                    .forEach(post -> outboxService.recordRefresh(SearchDocumentType.POST, post.getId()));
            meetupMapper.selectList(new LambdaQueryWrapper<Meetup>().eq(Meetup::getTopicId, aggregateId)
                            .isNull(Meetup::getCircleId))
                    .forEach(meetup -> outboxService.recordRefresh(SearchDocumentType.MEETUP, meetup.getId()));
        } else if (type == SearchDocumentType.CIRCLE) {
            postMapper.selectList(new LambdaQueryWrapper<Post>().eq(Post::getCircleId, aggregateId))
                    .forEach(post -> outboxService.recordRefresh(SearchDocumentType.POST, post.getId()));
            meetupMapper.selectList(new LambdaQueryWrapper<Meetup>().eq(Meetup::getCircleId, aggregateId))
                    .forEach(meetup -> outboxService.recordRefresh(SearchDocumentType.MEETUP, meetup.getId()));
        }
    }
}
