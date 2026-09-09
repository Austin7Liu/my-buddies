package com.austin.module.circle.controller;

import com.austin.common.model.ApiResponse;
import com.austin.common.model.PageResponse;
import com.austin.module.circle.controller.request.CreateCircleRequest;
import com.austin.module.circle.controller.request.UpdateCircleRequest;
import com.austin.module.circle.controller.response.CircleResponse;
import com.austin.module.circle.domain.Circle;
import com.austin.module.circle.service.CircleService;
import com.austin.module.profile.controller.response.ProfileSummaryResponse;
import com.austin.module.profile.service.ProfileService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class CircleController {
    private final CircleService circleService;
    private final ProfileService profileService;

    @GetMapping("/topics/{topicId}/circles")
    public ApiResponse<PageResponse<CircleResponse>> listPublic(@PathVariable @Positive long topicId,
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) long size) {
        return ApiResponse.success(responsePage(circleService.listPublic(topicId, page, size)));
    }

    @GetMapping("/circles/{circleId}")
    public ApiResponse<CircleResponse> getPublic(@PathVariable @Positive long circleId) {
        return ApiResponse.success(response(circleService.getPublic(circleId)));
    }

    @GetMapping("/circles/mine")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<PageResponse<CircleResponse>> listMine(Authentication auth,
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) long size) {
        return ApiResponse.success(responsePage(circleService.listMine(accountId(auth), page, size)));
    }

    @PostMapping("/circles")
    public ApiResponse<CircleResponse> create(Authentication auth, @Valid @RequestBody CreateCircleRequest request) {
        return ApiResponse.success(response(circleService.create(accountId(auth), request.topicId(),
                request.name(), request.description(), request.city(), request.district())));
    }

    @PutMapping("/circles/{circleId}")
    public ApiResponse<CircleResponse> update(Authentication auth, @PathVariable @Positive long circleId,
            @Valid @RequestBody UpdateCircleRequest request) {
        return ApiResponse.success(response(circleService.update(accountId(auth), circleId,
                request.name(), request.description(), request.city(), request.district())));
    }

    private PageResponse<CircleResponse> responsePage(IPage<Circle> page) {
        Map<Long, ProfileService.ProfileSummary> summaries = profileService.getSummaries(
                page.getRecords().stream().map(Circle::getCreatorAccountId).toList());
        return PageResponse.from(page, circle -> CircleResponse.from(circle,
                ProfileSummaryResponse.from(summaries.get(circle.getCreatorAccountId()))));
    }

    private CircleResponse response(Circle circle) {
        var summary = profileService.getSummaries(List.of(circle.getCreatorAccountId()))
                .get(circle.getCreatorAccountId());
        return CircleResponse.from(circle, ProfileSummaryResponse.from(summary));
    }

    private long accountId(Authentication auth) { return Long.parseLong(auth.getName()); }
}
