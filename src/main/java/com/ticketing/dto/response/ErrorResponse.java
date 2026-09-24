package com.ticketing.dto.response;

import java.util.List;

public record ErrorResponse(String code, String message, List<ErrorDetail> details) {}
