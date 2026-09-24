package com.ticketing.domain;

import com.ticketing.exception.AppException;
import com.ticketing.exception.ErrorCode;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class TicketStatusTransitionValidator {

    private static final Map<TicketStatus, Set<TicketStatus>> ALLOWED = Map.of(
            TicketStatus.OPEN, Set.of(TicketStatus.IN_PROGRESS, TicketStatus.CANCELLED),
            TicketStatus.IN_PROGRESS, Set.of(TicketStatus.RESOLVED, TicketStatus.CANCELLED),
            TicketStatus.RESOLVED, Set.of(TicketStatus.CLOSED),
            TicketStatus.CLOSED, Set.of(),
            TicketStatus.CANCELLED, Set.of());

    public void validate(TicketStatus from, TicketStatus to) {
        if (!ALLOWED.getOrDefault(from, Set.of()).contains(to)) {
            throw new AppException(
                    ErrorCode.INVALID_TRANSITION,
                    "Cannot transition ticket from " + from + " to " + to);
        }
    }
}
