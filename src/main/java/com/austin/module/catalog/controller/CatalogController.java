package com.austin.module.catalog.controller;

import com.austin.common.model.ApiResponse;
import com.austin.module.catalog.controller.response.CategoryResponse;
import com.austin.module.catalog.controller.response.TopicResponse;
import com.austin.module.catalog.domain.Topic;
import com.austin.module.catalog.service.CatalogService;
import com.austin.module.catalog.service.TopicFollowService;
import jakarta.validation.constraints.Positive;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class CatalogController {
    private final CatalogService catalogService;
    private final TopicFollowService followService;

    @GetMapping("/categories")
    public ApiResponse<List<CategoryResponse>> listCategories() {
        return ApiResponse.success(catalogService.listCategories(false).stream().map(CategoryResponse::from).toList());
    }

    @GetMapping("/categories/{categoryId}/topics")
    public ApiResponse<List<TopicResponse>> listTopics(
            Authentication authentication,
            @PathVariable @Positive long categoryId) {
        List<Topic> topics = catalogService.listTopics(categoryId, false);
        Long accountId = optionalAccountId(authentication);
        Set<Long> followedIds = accountId == null
                ? Set.of()
                : followService.followedTopicIds(accountId, topics.stream().map(Topic::getId).toList());
        return ApiResponse.success(topics.stream()
                .map(topic -> TopicResponse.from(topic, followedIds.contains(topic.getId())))
                .toList());
    }

    @GetMapping("/topics/{topicId}")
    public ApiResponse<TopicResponse> getTopic(
            Authentication authentication,
            @PathVariable @Positive long topicId) {
        var topic = catalogService.getTopic(topicId, false);
        Long accountId = optionalAccountId(authentication);
        return ApiResponse.success(TopicResponse.from(
                topic,
                accountId != null && followService.isFollowing(accountId, topicId)));
    }

    private Long optionalAccountId(Authentication authentication) {
        return authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())
                ? null
                : Long.parseLong(authentication.getName());
    }
}
