package com.austin.module.search.service;

import com.austin.module.meetup.domain.MeetupMode;
import com.austin.module.search.domain.SearchDocumentType;

public record SearchCriteria(
        String keyword,
        SearchDocumentType type,
        String city,
        MeetupMode meetupMode,
        int page,
        int size) {
}
