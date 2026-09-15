package com.austin.module.search.service;

import com.austin.module.catalog.domain.Topic;
import com.austin.module.catalog.mapper.TopicMapper;
import com.austin.module.circle.domain.Circle;
import com.austin.module.circle.mapper.CircleMapper;
import com.austin.module.meetup.domain.Meetup;
import com.austin.module.meetup.mapper.MeetupMapper;
import com.austin.module.post.domain.Post;
import com.austin.module.post.mapper.PostMapper;
import com.austin.module.search.domain.SearchDocument;
import com.austin.module.search.domain.SearchDocumentType;
import com.austin.module.search.domain.SearchOutboxEvent;
import com.austin.module.search.domain.SearchOutboxStatus;
import com.austin.module.search.mapper.SearchOutboxEventMapper;
import com.austin.module.search.provider.SearchProvider;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SearchIndexService {

    private final TopicMapper topicMapper;
    private final CircleMapper circleMapper;
    private final PostMapper postMapper;
    private final MeetupMapper meetupMapper;
    private final SearchDocumentFactory documentFactory;
    private final SearchProvider searchProvider;
    private final SearchOutboxEventMapper outboxMapper;
    private final Clock clock;

    public ReindexResult rebuild() {
        LocalDateTime rebuildStartedAt = LocalDateTime.now(clock);
        List<SearchDocument> documents = new ArrayList<>();
        appendDocuments(documents, SearchDocumentType.TOPIC,
                topicMapper.selectList(Wrappers.<Topic>lambdaQuery()).stream().map(Topic::getId).toList());
        appendDocuments(documents, SearchDocumentType.CIRCLE,
                circleMapper.selectList(Wrappers.<Circle>lambdaQuery()).stream().map(Circle::getId).toList());
        appendDocuments(documents, SearchDocumentType.POST,
                postMapper.selectList(Wrappers.<Post>lambdaQuery()).stream().map(Post::getId).toList());
        appendDocuments(documents, SearchDocumentType.MEETUP,
                meetupMapper.selectList(Wrappers.<Meetup>lambdaQuery()).stream().map(Meetup::getId).toList());

        ReindexResult result = searchProvider.rebuild(documents);
        replayEventsCreatedDuringRebuild(rebuildStartedAt);
        return result;
    }

    private void appendDocuments(List<SearchDocument> documents, SearchDocumentType type, List<Long> businessIds) {
        businessIds.forEach(businessId -> documentFactory.build(type, businessId).ifPresent(documents::add));
    }

    private void replayEventsCreatedDuringRebuild(LocalDateTime rebuildStartedAt) {
        outboxMapper.update(null, new LambdaUpdateWrapper<SearchOutboxEvent>()
                .ge(SearchOutboxEvent::getCreatedAt, rebuildStartedAt)
                .ne(SearchOutboxEvent::getStatus, SearchOutboxStatus.DEAD)
                .set(SearchOutboxEvent::getStatus, SearchOutboxStatus.PENDING)
                .set(SearchOutboxEvent::getNextRetryAt, LocalDateTime.now(clock))
                .set(SearchOutboxEvent::getLockedBy, null)
                .set(SearchOutboxEvent::getLockedAt, null));
    }
}
