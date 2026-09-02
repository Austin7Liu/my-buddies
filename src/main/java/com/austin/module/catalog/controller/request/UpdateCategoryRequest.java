package com.austin.module.catalog.controller.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateCategoryRequest(
        @NotBlank @Size(max = 64, message = "分类名称不能超过 64 个字符") String name,
        @Size(max = 255, message = "分类描述不能超过 255 个字符") String description,
        @NotNull @Min(value = 0, message = "排序值不能小于 0") Integer sortOrder) { }

