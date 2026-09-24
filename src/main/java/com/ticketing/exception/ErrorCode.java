package com.ticketing.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    VALIDATION_FAILED(HttpStatus.BAD_REQUEST),
    TICKET_NOT_FOUND(HttpStatus.NOT_FOUND),
    INVALID_TRANSITION(HttpStatus.UNPROCESSABLE_ENTITY),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR);

    private final HttpStatus status;

    ErrorCode(HttpStatus status) {
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
