package com.ticketing.dto.request;

import jakarta.validation.constraints.NotBlank;

public record AddCommentRequest(@NotBlank String body) {}
