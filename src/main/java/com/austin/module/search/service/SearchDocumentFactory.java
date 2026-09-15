package com.austin.module.search.service;

import com.austin.common.exception.ResourceNotFoundException;
import com.austin.module.catalog.domain.Topic;
import com.austin.module.catalog.service.CatalogService;
import com.austin.module.circle.domain.Circle;
import com.austin.module.circle.service.CircleService;
import com.austin.module.meetup.domain.Meetup;
import com.austin.module.meetup.domain.MeetupStatus;
import com.austin.module.meetup.mapper.MeetupMapper;
import com.austin.module.post.domain.Post;
import com.austin.module.post.domain.PostStatus;
import com.austin.module.post.mapper.PostMapper;
import com.austin.module.search.domain.SearchDocument;
import com.austin.module.search.domain.SearchDocumentType;
import java.util.EnumSet;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SearchDocumentFactory {

    private final PostMapper postMapper;
    private final MeetupMapper meetupMapper;
    private final CatalogService catalogService;
    private final CircleService circleService;

    public Optional<SearchDocument> build(SearchDocumentType type, long businessId) {
        return switch (type) {
            case TOPIC -> buildTopic(businessId);
            case CIRCLE -> buildCircle(businessId);
            case POST -> buildPost(businessId);
            case MEETUP -> buildMeetup(businessId);
        };
    }

    private Optional<SearchDocument> buildTopic(long topicId) {
        try {
            Topic topic = catalogService.getTopic(topicId, false);
            return Optional.of(new SearchDocument("TOPIC:" + topic.getId(), SearchDocumentType.TOPIC, topic.getId(),
                    topic.getName(), topic.getDescription(), topic.getId(), topic.getName(), null, null, null, null,
                    null, null, null, topic.getCreatedAt(), topic.getUpdatedAt(), true));
        } catch (ResourceNotFoundException exception) {
            return Optional.empty();
        }
    }

    private Optional<SearchDocument> buildCircle(long circleId) {
        try {
            Circle circle = circleService.getPublic(circleId);
            Topic topic = catalogService.getTopic(circle.getTopicId(), false);
            return Optional.of(new SearchDocument("CIRCLE:" + circle.getId(), SearchDocumentType.CIRCLE,
                    circle.getId(), circle.getName(), circle.getDescription(), topic.getId(), topic.getName(),
                    circle.getId(), circle.getName(), circle.getCreatorAccountId(), circle.getCity(),
                    circle.getDistrict(), null, null, circle.getCreatedAt(), circle.getUpdatedAt(), true));
        } catch (ResourceNotFoundException exception) {
            return Optional.empty();
        }
    }

    private Optional<SearchDocument> buildPost(long postId) {
        Post post = postMapper.selectById(postId);
        if (post == null || post.getStatus() != PostStatus.PUBLISHED) {
            return Optional.empty();
        }
        try {
            Topic topic = post.getTopicId() == null ? null : catalogService.getTopic(post.getTopicId(), false);
            Circle circle = post.getCircleId() == null ? null : circleService.getPublic(post.getCircleId());
            return Optional.of(new SearchDocument("POST:" + post.getId(), SearchDocumentType.POST, post.getId(), null,
                    post.getContent(), post.getTopicId(), topic == null ? null : topic.getName(), post.getCircleId(),
                    circle == null ? null : circle.getName(), post.getAuthorAccountId(), null, null, null, null,
                    post.getCreatedAt(), post.getUpdatedAt(), true));
        } catch (ResourceNotFoundException exception) {
            return Optional.empty();
        }
    }

    private Optional<SearchDocument> buildMeetup(long meetupId) {
        Meetup meetup = meetupMapper.selectById(meetupId);
        if (meetup == null || !EnumSet.of(MeetupStatus.OPEN, MeetupStatus.CONFIRMED,
                MeetupStatus.COMPLETED).contains(meetup.getStatus())) {
            return Optional.empty();
        }
        try {
            Topic topic = catalogService.getTopic(meetup.getTopicId(), false);
            Circle circle = meetup.getCircleId() == null ? null : circleService.getPublic(meetup.getCircleId());
            return Optional.of(new SearchDocument("MEETUP:" + meetup.getId(), SearchDocumentType.MEETUP,
                    meetup.getId(), meetup.getTitle(), meetup.getDescription(), meetup.getTopicId(), topic.getName(),
                    meetup.getCircleId(), circle == null ? null : circle.getName(), meetup.getCreatorAccountId(),
                    meetup.getCity(), meetup.getDistrict(), meetup.getMeetupMode().name(), meetup.getStartTime(),
                    meetup.getCreatedAt(), meetup.getUpdatedAt(), true));
        } catch (ResourceNotFoundException exception) {
            return Optional.empty();
        }
    }
}
