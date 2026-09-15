package com.austin.module.search.controller;

import com.austin.common.model.ApiResponse;
import com.austin.module.meetup.domain.MeetupMode;
import com.austin.module.search.domain.SearchDocumentType;
import com.austin.module.search.service.SearchCriteria;
import com.austin.module.search.service.SearchPage;
import com.austin.module.search.service.SearchService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/search")
public class SearchController {

    private final SearchService searchService;

    @GetMapping
    public ApiResponse<SearchPage> search(
            @RequestParam @NotBlank @Size(min = 2, max = 50) String keyword,
            @RequestParam(required = false) SearchDocumentType type,
            @RequestParam(required = false) @Size(max = 50) String city,
            @RequestParam(required = false) MeetupMode meetupMode,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ApiResponse.success(searchService.search(new SearchCriteria(
                keyword, type, city, meetupMode, page, size)));
    }
}
