package com.austin.module.circle.controller;

import com.austin.common.model.ApiResponse;
import com.austin.common.model.PageResponse;
import com.austin.module.circle.controller.response.CircleMemberResponse;
import com.austin.module.circle.controller.response.CircleResponse;
import com.austin.module.circle.domain.Circle;
import com.austin.module.circle.domain.CircleMember;
import com.austin.module.circle.service.CircleMembershipService;
import com.austin.module.profile.controller.response.ProfileSummaryResponse;
import com.austin.module.profile.service.ProfileService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class CircleMembershipController {

    private final CircleMembershipService membershipService;
    private final ProfileService profileService;

    @PostMapping("/circles/{circleId}/memberships")
    public ApiResponse<CircleMemberResponse> join(Authentication auth,
            @PathVariable @Positive long circleId) {
        return ApiResponse.success(memberResponse(membershipService.join(accountId(auth), circleId)));
    }

    @DeleteMapping("/circles/{circleId}/memberships/me")
    public ApiResponse<CircleMemberResponse> leave(Authentication auth,
            @PathVariable @Positive long circleId) {
        return ApiResponse.success(memberResponse(membershipService.leave(accountId(auth), circleId)));
    }

    @GetMapping("/circles/{circleId}/memberships/me")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<CircleMemberResponse> getMine(Authentication auth,
            @PathVariable @Positive long circleId) {
        return ApiResponse.success(memberResponse(membershipService.getMine(accountId(auth), circleId)));
    }

    @GetMapping("/circles/{circleId}/members")
    public ApiResponse<PageResponse<CircleMemberResponse>> listMembers(
            @PathVariable @Positive long circleId,
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) long size) {
        IPage<CircleMember> members = membershipService.listMembers(circleId, page, size);
        Map<Long, ProfileService.ProfileSummary> summaries = profileService.getSummaries(
                members.getRecords().stream().map(CircleMember::getAccountId).toList());
        return ApiResponse.success(PageResponse.from(members, member -> CircleMemberResponse.from(
                member,
                ProfileSummaryResponse.from(summaries.get(member.getAccountId())))));
    }

    @GetMapping("/me/circles")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<PageResponse<CircleResponse>> listMyCircles(
            Authentication auth,
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) long size) {
        IPage<Circle> circles = membershipService.listMyCircles(accountId(auth), page, size);
        Map<Long, ProfileService.ProfileSummary> summaries = profileService.getSummaries(
                circles.getRecords().stream().map(Circle::getCreatorAccountId).toList());
        return ApiResponse.success(PageResponse.from(circles, circle -> CircleResponse.from(
                circle,
                ProfileSummaryResponse.from(summaries.get(circle.getCreatorAccountId())))));
    }

    private CircleMemberResponse memberResponse(CircleMember member) {
        ProfileService.ProfileSummary summary = profileService.getSummaries(List.of(member.getAccountId()))
                .get(member.getAccountId());
        return CircleMemberResponse.from(member, ProfileSummaryResponse.from(summary));
    }

    private long accountId(Authentication auth) {
        return Long.parseLong(auth.getName());
    }
}
