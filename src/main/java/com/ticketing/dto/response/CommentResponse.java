package com.ticketing.dto.response;

import java.time.Instant;
import java.util.UUID;

public record CommentResponse(UUID id, String body, Instant createdAt) {}
