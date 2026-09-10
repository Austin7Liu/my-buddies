package com.austin.module.catalog.controller;

import com.austin.common.model.ApiResponse;
import com.austin.common.model.PageResponse;
import com.austin.module.catalog.controller.response.TopicFollowResponse;
import com.austin.module.catalog.controller.response.TopicResponse;
import com.austin.module.catalog.domain.Topic;
import com.austin.module.catalog.service.TopicFollowService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class TopicFollowController {

    private final TopicFollowService followService;

    @PutMapping("/topics/{topicId}/follow")
    public ApiResponse<TopicFollowResponse> follow(
            Authentication authentication,
            @PathVariable @Positive long topicId) {
        boolean followed = followService.follow(accountId(authentication), topicId);
        return ApiResponse.success(new TopicFollowResponse(topicId, followed));
    }

    @DeleteMapping("/topics/{topicId}/follow")
    public ApiResponse<TopicFollowResponse> unfollow(
            Authentication authentication,
            @PathVariable @Positive long topicId) {
        boolean followed = followService.unfollow(accountId(authentication), topicId);
        return ApiResponse.success(new TopicFollowResponse(topicId, followed));
    }

    @GetMapping("/me/followed-topics")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<PageResponse<TopicResponse>> listMine(
            Authentication authentication,
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) long size) {
        IPage<Topic> topics = followService.listMine(accountId(authentication), page, size);
        return ApiResponse.success(PageResponse.from(topics, topic -> TopicResponse.from(topic, true)));
    }

    private long accountId(Authentication authentication) {
        return Long.parseLong(authentication.getName());
    }
}
