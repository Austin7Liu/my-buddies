package com.austin.module.meetup.controller;

import com.austin.common.model.ApiResponse;
import com.austin.common.model.PageResponse;
import com.austin.module.meetup.controller.request.CreateMeetupRequest;
import com.austin.module.meetup.controller.request.MeetupApplicationRequest;
import com.austin.module.meetup.controller.request.MeetupCheckInRequest;
import com.austin.module.meetup.controller.request.ReasonRequest;
import com.austin.module.meetup.controller.request.UpdateMeetupRequest;
import com.austin.module.meetup.controller.response.MeetupParticipantResponse;
import com.austin.module.meetup.controller.response.MeetupCheckInResponse;
import com.austin.module.meetup.controller.response.MeetupResponse;
import com.austin.module.meetup.domain.Meetup;
import com.austin.module.meetup.domain.MeetupParticipant;
import com.austin.module.meetup.service.MeetupCommand;
import com.austin.module.meetup.service.MeetupCheckInService;
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
    private final MeetupCheckInService checkInService;
    private final ProfileService profileService;

    @GetMapping("/meetups")
    public ApiResponse<PageResponse<MeetupResponse>> listPublic(
            @RequestParam(required = false) @Positive Long topicId,
            @RequestParam(required = false) @Positive Long circleId,
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) long size) {
        return ApiResponse.success(responsePage(
                meetupService.listPublic(topicId, circleId, page, size), null));
    }

    @GetMapping("/topics/{topicId}/meetups")
    public ApiResponse<PageResponse<MeetupResponse>> listByTopic(
            @PathVariable @Positive long topicId,
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) long size) {
        return ApiResponse.success(responsePage(meetupService.listPublic(topicId, null, page, size), null));
    }

    @GetMapping("/circles/{circleId}/meetups")
    public ApiResponse<PageResponse<MeetupResponse>> listByCircle(
            @PathVariable @Positive long circleId,
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) long size) {
        return ApiResponse.success(responsePage(meetupService.listPublic(null, circleId, page, size), null));
    }

    @GetMapping("/meetups/{meetupId}")
    public ApiResponse<MeetupResponse> get(
            Authentication authentication,
            @PathVariable @Positive long meetupId) {
        Long viewerId = authentication == null ? null : accountId(authentication);
        Meetup meetup = meetupService.getVisible(viewerId, meetupId);
        return ApiResponse.success(response(meetup, meetupService.canSeePrivateDetails(viewerId, meetup)));
    }

    @GetMapping("/meetups/mine")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<PageResponse<MeetupResponse>> listMine(
            Authentication authentication,
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) long size) {
        long viewerId = accountId(authentication);
        return ApiResponse.success(responsePage(meetupService.listMine(viewerId, page, size), viewerId));
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

    @PostMapping("/meetups/{meetupId}/complete")
    public ApiResponse<MeetupResponse> complete(
            Authentication authentication,
            @PathVariable @Positive long meetupId) {
        return ApiResponse.success(response(meetupService.complete(accountId(authentication), meetupId), true));
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
        return ApiResponse.success(participantResponse(
                meetupService.apply(accountId(authentication), meetupId, request.message())));
    }

    @GetMapping("/meetups/{meetupId}/applications")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<PageResponse<MeetupParticipantResponse>> listApplications(
            Authentication authentication,
            @PathVariable @Positive long meetupId,
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) long size) {
        return ApiResponse.success(participantPage(
                meetupService.listApplications(accountId(authentication), meetupId, page, size)));
    }

    @PostMapping("/meetups/{meetupId}/applications/{applicantId}/accept")
    public ApiResponse<MeetupParticipantResponse> accept(
            Authentication authentication,
            @PathVariable @Positive long meetupId,
            @PathVariable @Positive long applicantId) {
        return ApiResponse.success(participantResponse(
                meetupService.decide(accountId(authentication), meetupId, applicantId, true, null)));
    }

    @PostMapping("/meetups/{meetupId}/applications/{applicantId}/reject")
    public ApiResponse<MeetupParticipantResponse> reject(
            Authentication authentication,
            @PathVariable @Positive long meetupId,
            @PathVariable @Positive long applicantId,
            @Valid @RequestBody ReasonRequest request) {
        return ApiResponse.success(participantResponse(
                meetupService.decide(accountId(authentication), meetupId, applicantId, false, request.reason())));
    }

    @PostMapping("/meetups/{meetupId}/withdraw")
    public ApiResponse<MeetupParticipantResponse> withdraw(
            Authentication authentication,
            @PathVariable @Positive long meetupId,
            @Valid @RequestBody ReasonRequest request) {
        return ApiResponse.success(participantResponse(
                meetupService.withdraw(accountId(authentication), meetupId, request.reason())));
    }

    @PostMapping("/meetups/{meetupId}/check-ins")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<MeetupCheckInResponse> checkIn(
            Authentication authentication,
            @PathVariable @Positive long meetupId,
            @Valid @RequestBody MeetupCheckInRequest request) {
        return ApiResponse.success(MeetupCheckInResponse.from(checkInService.checkIn(
                accountId(authentication), meetupId, request.latitude(), request.longitude())));
    }

    @GetMapping("/meetups/{meetupId}/check-ins/me")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<MeetupCheckInResponse> getMyCheckIn(
            Authentication authentication,
            @PathVariable @Positive long meetupId) {
        return ApiResponse.success(MeetupCheckInResponse.from(
                checkInService.getMine(accountId(authentication), meetupId)));
    }

    @GetMapping("/meetups/{meetupId}/check-ins")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<PageResponse<MeetupCheckInResponse>> listCheckIns(
            Authentication authentication,
            @PathVariable @Positive long meetupId,
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) long size) {
        return ApiResponse.success(PageResponse.from(
                checkInService.list(accountId(authentication), meetupId, page, size),
                MeetupCheckInResponse::from));
    }

    private MeetupResponse response(Meetup meetup, boolean exposeAddress) {
        var summary = profileService.getSummaries(List.of(meetup.getCreatorAccountId()))
                .get(meetup.getCreatorAccountId());
        return response(meetup, exposeAddress, ProfileSummaryResponse.from(summary));
    }

    private MeetupResponse response(
            Meetup meetup,
            boolean exposeAddress,
            ProfileSummaryResponse creator) {
        return MeetupResponse.from(meetup, meetupService.findOnlineDetail(meetup.getId()),
                creator, meetupService.acceptedCount(meetup.getId()), exposeAddress);
    }

    private PageResponse<MeetupResponse> responsePage(IPage<Meetup> page, Long viewerId) {
        Map<Long, ProfileService.ProfileSummary> summaries = profileService.getSummaries(
                page.getRecords().stream().map(Meetup::getCreatorAccountId).toList());
        return PageResponse.from(page, meetup -> response(meetup,
                viewerId != null && meetupService.canSeePrivateDetails(viewerId, meetup),
                ProfileSummaryResponse.from(summaries.get(meetup.getCreatorAccountId()))));
    }

    private PageResponse<MeetupParticipantResponse> participantPage(IPage<MeetupParticipant> page) {
        Map<Long, ProfileService.ProfileSummary> summaries = profileService.getSummaries(
                page.getRecords().stream().map(MeetupParticipant::getAccountId).toList());
        return PageResponse.from(page, participant -> MeetupParticipantResponse.from(participant,
                ProfileSummaryResponse.from(summaries.get(participant.getAccountId()))));
    }

    private MeetupParticipantResponse participantResponse(MeetupParticipant participant) {
        var summary = profileService.getSummaries(List.of(participant.getAccountId()))
                .get(participant.getAccountId());
        return MeetupParticipantResponse.from(participant, ProfileSummaryResponse.from(summary));
    }

    private MeetupCommand command(CreateMeetupRequest request) {
        return new MeetupCommand(request.meetupMode(), request.title(), request.description(), request.startTime(), request.endTime(),
                request.applicationDeadline(), request.city(), request.district(), request.locationName(),
                request.address(), request.locationLatitude(), request.locationLongitude(), request.checkInRadiusMeters(),
                request.onlinePlatform(), request.serverRegion(), request.accessInstructions(),
                request.capacity(), request.minimumAge(), request.maximumAge(),
                request.genderRequirement(), request.skillRequirement());
    }

    private MeetupCommand command(UpdateMeetupRequest request) {
        return new MeetupCommand(request.meetupMode(), request.title(), request.description(), request.startTime(), request.endTime(),
                request.applicationDeadline(), request.city(), request.district(), request.locationName(),
                request.address(), request.locationLatitude(), request.locationLongitude(), request.checkInRadiusMeters(),
                request.onlinePlatform(), request.serverRegion(), request.accessInstructions(),
                request.capacity(), request.minimumAge(), request.maximumAge(),
                request.genderRequirement(), request.skillRequirement());
    }

    private long accountId(Authentication authentication) {
        return Long.parseLong(authentication.getName());
    }
}
