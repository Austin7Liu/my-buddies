package com.austin.module.catalog.controller.response;

public record TopicFollowResponse(
        Long topicId,
        boolean followedByMe) {
}
