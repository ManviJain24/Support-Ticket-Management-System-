package com.ticketing.dto.request;

import com.ticketing.domain.TicketStatus;
import jakarta.validation.constraints.NotNull;

public record TransitionTicketRequest(@NotNull TicketStatus status) {}
