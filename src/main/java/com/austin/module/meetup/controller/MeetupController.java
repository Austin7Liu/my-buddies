package com.austin.module.meetup.controller;

import com.austin.common.model.ApiResponse;
import com.austin.common.model.PageResponse;
import com.austin.module.meetup.controller.request.CreateMeetupRequest;
import com.austin.module.meetup.controller.request.MeetupApplicationRequest;
import com.austin.module.meetup.controller.request.ReasonRequest;
import com.austin.module.meetup.controller.request.UpdateMeetupRequest;
import com.austin.module.meetup.controller.response.MeetupParticipantResponse;
import com.austin.module.meetup.controller.response.MeetupResponse;
import com.austin.module.meetup.domain.Meetup;
import com.austin.module.meetup.service.MeetupCommand;
import com.austin.module.meetup.service.MeetupService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
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
public class MeetupController {

    private final MeetupService meetupService;

    @GetMapping("/meetups")
    public ApiResponse<PageResponse<MeetupResponse>> listPublic(
            @RequestParam(required = false) @Positive Long topicId,
            @RequestParam(required = false) @Positive Long circleId,
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) long size) {
        return ApiResponse.success(PageResponse.from(meetupService.listPublic(topicId, circleId, page, size),
                meetup -> response(meetup, false)));
    }

    @GetMapping("/topics/{topicId}/meetups")
    public ApiResponse<PageResponse<MeetupResponse>> listByTopic(
            @PathVariable @Positive long topicId,
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) long size) {
        return ApiResponse.success(PageResponse.from(meetupService.listPublic(topicId, null, page, size),
                meetup -> response(meetup, false)));
    }

    @GetMapping("/circles/{circleId}/meetups")
    public ApiResponse<PageResponse<MeetupResponse>> listByCircle(
            @PathVariable @Positive long circleId,
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) long size) {
        return ApiResponse.success(PageResponse.from(meetupService.listPublic(null, circleId, page, size),
                meetup -> response(meetup, false)));
    }

    @GetMapping("/meetups/{meetupId}")
    public ApiResponse<MeetupResponse> get(
            Authentication authentication,
            @PathVariable @Positive long meetupId) {
        Long viewerId = authentication == null ? null : accountId(authentication);
        Meetup meetup = meetupService.getVisible(viewerId, meetupId);
        return ApiResponse.success(response(meetup, meetupService.canSeeExactAddress(viewerId, meetup)));
    }

    @GetMapping("/meetups/mine")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<PageResponse<MeetupResponse>> listMine(
            Authentication authentication,
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) long size) {
        return ApiResponse.success(PageResponse.from(
                meetupService.listMine(accountId(authentication), page, size), meetup -> response(meetup, true)));
    }

    @PostMapping("/meetups")
    public ApiResponse<MeetupResponse> create(
            Authentication authentication,
            @Valid @RequestBody CreateMeetupRequest request) {
        Meetup meetup = meetupService.create(accountId(authentication), request.topicId(), request.circleId(),
                command(request));
        return ApiResponse.success(response(meetup, true));
    }

    @PutMapping("/meetups/{meetupId}")
    public ApiResponse<MeetupResponse> update(
            Authentication authentication,
            @PathVariable @Positive long meetupId,
            @Valid @RequestBody UpdateMeetupRequest request) {
        Meetup meetup = meetupService.update(accountId(authentication), meetupId, command(request));
        return ApiResponse.success(response(meetup, true));
    }

    @PostMapping("/meetups/{meetupId}/publish")
    public ApiResponse<MeetupResponse> publish(
            Authentication authentication,
            @PathVariable @Positive long meetupId) {
        return ApiResponse.success(response(meetupService.publish(accountId(authentication), meetupId), true));
    }

    @PostMapping("/meetups/{meetupId}/confirm")
    public ApiResponse<MeetupResponse> confirm(
            Authentication authentication,
            @PathVariable @Positive long meetupId) {
        return ApiResponse.success(response(meetupService.confirm(accountId(authentication), meetupId), true));
    }

    @PostMapping("/meetups/{meetupId}/cancel")
    public ApiResponse<MeetupResponse> cancel(
            Authentication authentication,
            @PathVariable @Positive long meetupId,
            @Valid @RequestBody ReasonRequest request) {
        return ApiResponse.success(response(
                meetupService.cancel(accountId(authentication), meetupId, request.reason()), true));
    }

    @PostMapping("/meetups/{meetupId}/applications")
    public ApiResponse<MeetupParticipantResponse> apply(
            Authentication authentication,
            @PathVariable @Positive long meetupId,
            @Valid @RequestBody MeetupApplicationRequest request) {
        return ApiResponse.success(MeetupParticipantResponse.from(
                meetupService.apply(accountId(authentication), meetupId, request.message())));
    }

    @GetMapping("/meetups/{meetupId}/applications")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<PageResponse<MeetupParticipantResponse>> listApplications(
            Authentication authentication,
            @PathVariable @Positive long meetupId,
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) long size) {
        return ApiResponse.success(PageResponse.from(
                meetupService.listApplications(accountId(authentication), meetupId, page, size),
                MeetupParticipantResponse::from));
    }

    @PostMapping("/meetups/{meetupId}/applications/{applicantId}/accept")
    public ApiResponse<MeetupParticipantResponse> accept(
            Authentication authentication,
            @PathVariable @Positive long meetupId,
            @PathVariable @Positive long applicantId) {
        return ApiResponse.success(MeetupParticipantResponse.from(
                meetupService.decide(accountId(authentication), meetupId, applicantId, true, null)));
    }

    @PostMapping("/meetups/{meetupId}/applications/{applicantId}/reject")
    public ApiResponse<MeetupParticipantResponse> reject(
            Authentication authentication,
            @PathVariable @Positive long meetupId,
            @PathVariable @Positive long applicantId,
            @Valid @RequestBody ReasonRequest request) {
        return ApiResponse.success(MeetupParticipantResponse.from(
                meetupService.decide(accountId(authentication), meetupId, applicantId, false, request.reason())));
    }

    @PostMapping("/meetups/{meetupId}/withdraw")
    public ApiResponse<MeetupParticipantResponse> withdraw(
            Authentication authentication,
            @PathVariable @Positive long meetupId,
            @Valid @RequestBody ReasonRequest request) {
        return ApiResponse.success(MeetupParticipantResponse.from(
                meetupService.withdraw(accountId(authentication), meetupId, request.reason())));
    }

    private MeetupResponse response(Meetup meetup, boolean exposeAddress) {
        return MeetupResponse.from(meetup, meetupService.acceptedCount(meetup.getId()), exposeAddress);
    }

    private MeetupCommand command(CreateMeetupRequest request) {
        return new MeetupCommand(request.title(), request.description(), request.startTime(), request.endTime(),
                request.applicationDeadline(), request.city(), request.district(), request.locationName(),
                request.address(), request.capacity(), request.minimumAge(), request.maximumAge(),
                request.genderRequirement(), request.skillRequirement());
    }

    private MeetupCommand command(UpdateMeetupRequest request) {
        return new MeetupCommand(request.title(), request.description(), request.startTime(), request.endTime(),
                request.applicationDeadline(), request.city(), request.district(), request.locationName(),
                request.address(), request.capacity(), request.minimumAge(), request.maximumAge(),
                request.genderRequirement(), request.skillRequirement());
    }

    private long accountId(Authentication authentication) {
        return Long.parseLong(authentication.getName());
    }
}
