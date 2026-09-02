package com.austin.module.catalog.controller.request;

import jakarta.validation.constraints.NotNull;

public record UpdateEnabledRequest(@NotNull(message = "启用状态不能为空") Boolean enabled) { }

