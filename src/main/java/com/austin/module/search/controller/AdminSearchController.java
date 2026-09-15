package com.austin.module.search.controller;

import com.austin.common.model.ApiResponse;
import com.austin.module.search.service.ReindexResult;
import com.austin.module.search.service.SearchIndexService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/search")
public class AdminSearchController {

    private final SearchIndexService searchIndexService;

    @PostMapping("/reindex")
    @PreAuthorize("hasAnyRole('CONTENT_ADMIN', 'SUPER_ADMIN')")
    public ApiResponse<ReindexResult> reindex() {
        return ApiResponse.success(searchIndexService.rebuild());
    }
}
