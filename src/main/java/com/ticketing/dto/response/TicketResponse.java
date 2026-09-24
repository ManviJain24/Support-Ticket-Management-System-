package com.ticketing.dto.response;

import com.ticketing.domain.Priority;
import com.ticketing.domain.TicketStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record TicketResponse(
        UUID id,
        String title,
        String description,
        TicketStatus status,
        Priority priority,
        String assignee,
        Instant createdAt,
        Instant updatedAt,
        List<CommentResponse> comments) {}
