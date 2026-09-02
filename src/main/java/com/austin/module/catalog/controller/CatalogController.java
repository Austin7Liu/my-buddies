package com.austin.module.catalog.controller;

import com.austin.common.model.ApiResponse;
import com.austin.module.catalog.controller.response.CategoryResponse;
import com.austin.module.catalog.controller.response.TopicResponse;
import com.austin.module.catalog.service.CatalogService;
import jakarta.validation.constraints.Positive;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
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

    @GetMapping("/categories")
    public ApiResponse<List<CategoryResponse>> listCategories() {
        return ApiResponse.success(catalogService.listCategories(false).stream().map(CategoryResponse::from).toList());
    }

    @GetMapping("/categories/{categoryId}/topics")
    public ApiResponse<List<TopicResponse>> listTopics(@PathVariable @Positive long categoryId) {
        return ApiResponse.success(catalogService.listTopics(categoryId, false).stream().map(TopicResponse::from).toList());
    }

    @GetMapping("/topics/{topicId}")
    public ApiResponse<TopicResponse> getTopic(@PathVariable @Positive long topicId) {
        return ApiResponse.success(TopicResponse.from(catalogService.getTopic(topicId, false)));
    }
}

