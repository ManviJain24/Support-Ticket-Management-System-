package com.ticketing.dto.request;

import com.ticketing.domain.Priority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateTicketRequest(
        @NotBlank @Size(max = 200) String title,
        @NotBlank String description,
        @NotNull Priority priority,
        @Size(max = 200) String assignee) {}
