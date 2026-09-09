package com.austin.module.meetup.controller;

import com.austin.common.model.ApiResponse;
import com.austin.common.model.PageResponse;
import com.austin.module.meetup.controller.request.ReasonRequest;
import com.austin.module.meetup.controller.response.MeetupResponse;
import com.austin.module.meetup.domain.MeetupStatus;
import com.austin.module.meetup.domain.Meetup;
import com.austin.module.meetup.service.MeetupService;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@PreAuthorize("hasRole('CONTENT_ADMIN')")
@RequestMapping("/api/v1/admin/meetups")
public class AdminMeetupController {

    private final MeetupService meetupService;
    private final ProfileService profileService;

    @GetMapping
    public ApiResponse<PageResponse<MeetupResponse>> list(
            @RequestParam(required = false) MeetupStatus status,
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) long size) {
        return ApiResponse.success(responsePage(meetupService.listForAdmin(status, page, size)));
    }

    @PostMapping("/{meetupId}/terminate")
    public ApiResponse<MeetupResponse> terminate(
            Authentication authentication,
            @PathVariable @Positive long meetupId,
            @Valid @RequestBody ReasonRequest request) {
        var meetup = meetupService.terminate(Long.parseLong(authentication.getName()), meetupId, request.reason());
        return ApiResponse.success(response(meetup));
    }

    private PageResponse<MeetupResponse> responsePage(IPage<Meetup> page) {
        Map<Long, ProfileService.ProfileSummary> summaries = profileService.getSummaries(
                page.getRecords().stream().map(Meetup::getCreatorAccountId).toList());
        return PageResponse.from(page, meetup -> MeetupResponse.from(
                meetup, meetupService.findOnlineDetail(meetup.getId()),
                ProfileSummaryResponse.from(summaries.get(meetup.getCreatorAccountId())),
                meetupService.acceptedCount(meetup.getId()), false));
    }

    private MeetupResponse response(Meetup meetup) {
        var summary = profileService.getSummaries(List.of(meetup.getCreatorAccountId()))
                .get(meetup.getCreatorAccountId());
        return MeetupResponse.from(meetup, meetupService.findOnlineDetail(meetup.getId()),
                ProfileSummaryResponse.from(summary), meetupService.acceptedCount(meetup.getId()), false);
    }
}
