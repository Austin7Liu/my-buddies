package com.austin.module.notification.controller;

import com.austin.common.model.ApiResponse;
import com.austin.common.model.PageResponse;
import com.austin.module.notification.controller.response.NotificationResponse;
import com.austin.module.notification.controller.response.ReadAllResponse;
import com.austin.module.notification.controller.response.UnreadCountResponse;
import com.austin.module.notification.service.NotificationService;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ApiResponse<PageResponse<NotificationResponse>> list(
            Authentication authentication,
            @RequestParam(defaultValue = "false") boolean unreadOnly,
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) long size) {
        return ApiResponse.success(PageResponse.from(notificationService.listMine(
                accountId(authentication), unreadOnly, page, size), NotificationResponse::from));
    }

    @GetMapping("/unread-count")
    public ApiResponse<UnreadCountResponse> unreadCount(Authentication authentication) {
        return ApiResponse.success(new UnreadCountResponse(notificationService.unreadCount(accountId(authentication))));
    }

    @PostMapping("/{notificationId}/read")
    public ApiResponse<NotificationResponse> read(
            Authentication authentication,
            @PathVariable @Positive long notificationId) {
        return ApiResponse.success(NotificationResponse.from(
                notificationService.read(accountId(authentication), notificationId)));
    }

    @PostMapping("/read-all")
    public ApiResponse<ReadAllResponse> readAll(Authentication authentication) {
        return ApiResponse.success(new ReadAllResponse(notificationService.readAll(accountId(authentication))));
    }

    private long accountId(Authentication authentication) {
        return Long.parseLong(authentication.getName());
    }
}
