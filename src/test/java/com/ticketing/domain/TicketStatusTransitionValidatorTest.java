package com.ticketing.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ticketing.exception.AppException;
import com.ticketing.exception.ErrorCode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class TicketStatusTransitionValidatorTest {

    private final TicketStatusTransitionValidator validator = new TicketStatusTransitionValidator();

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({
        "OPEN, IN_PROGRESS",
        "OPEN, CANCELLED",
        "IN_PROGRESS, RESOLVED",
        "IN_PROGRESS, CANCELLED",
        "RESOLVED, CLOSED"
    })
    void shouldAllow_whenTransitionIsValid(TicketStatus from, TicketStatus to) {
        assertThatCode(() -> validator.validate(from, to)).doesNotThrowAnyException();
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({
        "CLOSED, OPEN",
        "RESOLVED, OPEN",
        "CANCELLED, OPEN",
        "CLOSED, IN_PROGRESS",
        "CLOSED, RESOLVED",
        "CLOSED, CANCELLED",
        "CLOSED, CLOSED",
        "CANCELLED, IN_PROGRESS",
        "CANCELLED, RESOLVED",
        "CANCELLED, CLOSED",
        "CANCELLED, CANCELLED",
        "RESOLVED, IN_PROGRESS",
        "RESOLVED, CANCELLED",
        "RESOLVED, RESOLVED",
        "OPEN, RESOLVED",
        "OPEN, CLOSED",
        "OPEN, OPEN",
        "IN_PROGRESS, OPEN",
        "IN_PROGRESS, CLOSED",
        "IN_PROGRESS, IN_PROGRESS"
    })
    void shouldReject_whenTransitionIsInvalid(TicketStatus from, TicketStatus to) {
        assertThatThrownBy(() -> validator.validate(from, to))
                .isInstanceOfSatisfying(AppException.class, exception -> {
                    assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_TRANSITION);
                    assertThat(exception.getMessage())
                            .isEqualTo("Cannot transition ticket from " + from + " to " + to);
                });
    }
}
