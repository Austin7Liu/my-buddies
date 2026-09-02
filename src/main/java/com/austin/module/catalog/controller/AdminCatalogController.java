package com.austin.module.catalog.controller;

import com.austin.common.model.ApiResponse;
import com.austin.module.catalog.controller.request.CreateCategoryRequest;
import com.austin.module.catalog.controller.request.CreateTopicRequest;
import com.austin.module.catalog.controller.request.UpdateCategoryRequest;
import com.austin.module.catalog.controller.request.UpdateEnabledRequest;
import com.austin.module.catalog.controller.request.UpdateTopicRequest;
import com.austin.module.catalog.controller.response.CategoryResponse;
import com.austin.module.catalog.controller.response.TopicResponse;
import com.austin.module.catalog.service.CatalogService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@PreAuthorize("hasRole('CONTENT_ADMIN')")
@RequestMapping("/api/v1/admin/catalog")
public class AdminCatalogController {
    private final CatalogService catalogService;

    @GetMapping("/categories")
    public ApiResponse<List<CategoryResponse>> listCategories() {
        return ApiResponse.success(catalogService.listCategories(true).stream().map(CategoryResponse::from).toList());
    }

    @GetMapping("/categories/{categoryId}/topics")
    public ApiResponse<List<TopicResponse>> listTopics(@PathVariable @Positive long categoryId) {
        return ApiResponse.success(catalogService.listTopics(categoryId, true).stream().map(TopicResponse::from).toList());
    }

    @PostMapping("/categories")
    public ApiResponse<CategoryResponse> createCategory(Authentication auth, @Valid @RequestBody CreateCategoryRequest request) {
        return ApiResponse.success(CategoryResponse.from(catalogService.createCategory(accountId(auth), request.code(),
                request.name(), request.description(), request.sortOrder())));
    }

    @PutMapping("/categories/{categoryId}")
    public ApiResponse<CategoryResponse> updateCategory(Authentication auth, @PathVariable @Positive long categoryId,
            @Valid @RequestBody UpdateCategoryRequest request) {
        return ApiResponse.success(CategoryResponse.from(catalogService.updateCategory(accountId(auth), categoryId,
                request.name(), request.description(), request.sortOrder())));
    }

    @PatchMapping("/categories/{categoryId}/enabled")
    public ApiResponse<CategoryResponse> setCategoryEnabled(Authentication auth, @PathVariable @Positive long categoryId,
            @Valid @RequestBody UpdateEnabledRequest request) {
        return ApiResponse.success(CategoryResponse.from(catalogService.setCategoryEnabled(
                accountId(auth), categoryId, request.enabled())));
    }

    @PostMapping("/categories/{categoryId}/topics")
    public ApiResponse<TopicResponse> createTopic(Authentication auth, @PathVariable @Positive long categoryId,
            @Valid @RequestBody CreateTopicRequest request) {
        return ApiResponse.success(TopicResponse.from(catalogService.createTopic(accountId(auth), categoryId,
                request.code(), request.name(), request.description(), request.sortOrder())));
    }

    @PutMapping("/topics/{topicId}")
    public ApiResponse<TopicResponse> updateTopic(Authentication auth, @PathVariable @Positive long topicId,
            @Valid @RequestBody UpdateTopicRequest request) {
        return ApiResponse.success(TopicResponse.from(catalogService.updateTopic(accountId(auth), topicId,
                request.name(), request.description(), request.sortOrder())));
    }

    @PatchMapping("/topics/{topicId}/enabled")
    public ApiResponse<TopicResponse> setTopicEnabled(Authentication auth, @PathVariable @Positive long topicId,
            @Valid @RequestBody UpdateEnabledRequest request) {
        return ApiResponse.success(TopicResponse.from(catalogService.setTopicEnabled(
                accountId(auth), topicId, request.enabled())));
    }

    private long accountId(Authentication authentication) { return Long.parseLong(authentication.getName()); }
}

