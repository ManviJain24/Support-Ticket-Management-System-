package com.ticketing.controller;

import com.ticketing.domain.TicketStatus;
import com.ticketing.dto.request.AddCommentRequest;
import com.ticketing.dto.request.CreateTicketRequest;
import com.ticketing.dto.request.TransitionTicketRequest;
import com.ticketing.dto.request.UpdateTicketRequest;
import com.ticketing.dto.response.CommentResponse;
import com.ticketing.dto.response.PageResponse;
import com.ticketing.dto.response.TicketResponse;
import com.ticketing.dto.response.TicketSummaryResponse;
import com.ticketing.exception.AppException;
import com.ticketing.exception.ErrorCode;
import com.ticketing.service.TicketService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tickets")
public class TicketController {

    private static final Set<String> SORT_FIELDS =
            Set.of("createdAt", "updatedAt", "title", "status", "priority");

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @PostMapping
    public ResponseEntity<TicketResponse> create(
            @Valid @RequestBody CreateTicketRequest request) {
        TicketResponse response = ticketService.create(request);
        return ResponseEntity.created(URI.create("/api/v1/tickets/" + response.id()))
                .body(response);
    }

    @GetMapping
    public PageResponse<TicketSummaryResponse> list(
            @RequestParam(required = false, name = "q") String keyword,
            @RequestParam(required = false) TicketStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {
        if (page < 0 || size < 1 || size > 100) {
            throw validationException();
        }

        String[] sortParts = sort.split(",", -1);
        if (sortParts.length != 2 || !SORT_FIELDS.contains(sortParts[0])) {
            throw validationException();
        }

        Sort.Direction direction;
        try {
            direction = Sort.Direction.fromString(sortParts[1]);
        } catch (IllegalArgumentException exception) {
            throw validationException();
        }

        return ticketService.list(
                keyword,
                status,
                PageRequest.of(page, size, Sort.by(direction, sortParts[0])));
    }

    @GetMapping("/{id}")
    public TicketResponse getById(@PathVariable UUID id) {
        return ticketService.getById(id);
    }

    @PatchMapping("/{id}")
    public TicketResponse update(
            @PathVariable UUID id, @Valid @RequestBody UpdateTicketRequest request) {
        return ticketService.update(id, request);
    }

    @PostMapping("/{id}/comments")
    public ResponseEntity<CommentResponse> addComment(
            @PathVariable UUID id, @Valid @RequestBody AddCommentRequest request) {
        CommentResponse response = ticketService.addComment(id, request);
        return ResponseEntity.created(
                        URI.create("/api/v1/tickets/" + id + "/comments/" + response.id()))
                .body(response);
    }

    @PostMapping("/{id}/status")
    public TicketResponse transition(
            @PathVariable UUID id, @Valid @RequestBody TransitionTicketRequest request) {
        return ticketService.transition(id, request.status());
    }

    private AppException validationException() {
        return new AppException(ErrorCode.VALIDATION_FAILED, "Request validation failed");
    }
}
