package com.austin.module.search.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.austin.module.circle.domain.Circle;
import com.austin.module.circle.mapper.CircleMapper;
import com.austin.module.meetup.domain.Meetup;
import com.austin.module.meetup.mapper.MeetupMapper;
import com.austin.module.post.domain.Post;
import com.austin.module.post.mapper.PostMapper;
import com.austin.module.search.domain.SearchDocumentType;
import java.util.List;
import org.junit.jupiter.api.Test;

class SearchCascadeServiceTests {

    @Test
    void topicCascadeRefreshesTopicAndRelatedDocuments() {
        CircleMapper circleMapper = mock(CircleMapper.class);
        PostMapper postMapper = mock(PostMapper.class);
        MeetupMapper meetupMapper = mock(MeetupMapper.class);
        SearchOutboxService outboxService = mock(SearchOutboxService.class);
        when(circleMapper.selectList(any())).thenReturn(List.of(Circle.builder().id(20L).build()));
        when(postMapper.selectList(any())).thenReturn(List.of(Post.builder().id(30L).build()));
        when(meetupMapper.selectList(any())).thenReturn(List.of(Meetup.builder().id(40L).build()));

        new SearchCascadeService(circleMapper, postMapper, meetupMapper, outboxService)
                .expand(SearchDocumentType.TOPIC, 10L);

        verify(outboxService).recordRefresh(SearchDocumentType.TOPIC, 10L);
        verify(outboxService).recordCascade(SearchDocumentType.CIRCLE, 20L);
        verify(outboxService).recordRefresh(SearchDocumentType.POST, 30L);
        verify(outboxService).recordRefresh(SearchDocumentType.MEETUP, 40L);
    }

    @Test
    void circleCascadeRefreshesCircleAndItsContent() {
        CircleMapper circleMapper = mock(CircleMapper.class);
        PostMapper postMapper = mock(PostMapper.class);
        MeetupMapper meetupMapper = mock(MeetupMapper.class);
        SearchOutboxService outboxService = mock(SearchOutboxService.class);
        when(postMapper.selectList(any())).thenReturn(List.of(Post.builder().id(30L).build()));
        when(meetupMapper.selectList(any())).thenReturn(List.of(Meetup.builder().id(40L).build()));

        new SearchCascadeService(circleMapper, postMapper, meetupMapper, outboxService)
                .expand(SearchDocumentType.CIRCLE, 20L);

        verify(outboxService).recordRefresh(SearchDocumentType.CIRCLE, 20L);
        verify(outboxService).recordRefresh(SearchDocumentType.POST, 30L);
        verify(outboxService).recordRefresh(SearchDocumentType.MEETUP, 40L);
    }
}
