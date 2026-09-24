package com.ticketing.service;

import com.ticketing.domain.Comment;
import com.ticketing.domain.Ticket;
import com.ticketing.domain.TicketStatus;
import com.ticketing.domain.TicketStatusTransitionValidator;
import com.ticketing.dto.request.AddCommentRequest;
import com.ticketing.dto.request.CreateTicketRequest;
import com.ticketing.dto.request.UpdateTicketRequest;
import com.ticketing.dto.response.CommentResponse;
import com.ticketing.dto.response.PageResponse;
import com.ticketing.dto.response.TicketResponse;
import com.ticketing.dto.response.TicketSummaryResponse;
import com.ticketing.exception.AppException;
import com.ticketing.exception.ErrorCode;
import com.ticketing.mapper.TicketMapper;
import com.ticketing.repository.CommentRepository;
import com.ticketing.repository.TicketRepository;
import java.util.Locale;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TicketService {

    private final TicketRepository ticketRepository;
    private final CommentRepository commentRepository;
    private final TicketMapper ticketMapper;
    private final TicketStatusTransitionValidator statusTransitionValidator;

    public TicketService(
            TicketRepository ticketRepository,
            CommentRepository commentRepository,
            TicketMapper ticketMapper,
            TicketStatusTransitionValidator statusTransitionValidator) {
        this.ticketRepository = ticketRepository;
        this.commentRepository = commentRepository;
        this.ticketMapper = ticketMapper;
        this.statusTransitionValidator = statusTransitionValidator;
    }

    @Transactional
    public TicketResponse create(CreateTicketRequest request) {
        Ticket ticket = Ticket.builder()
                .title(request.title())
                .description(request.description())
                .priority(request.priority())
                .assignee(request.assignee())
                .status(TicketStatus.OPEN)
                .build();
        return ticketMapper.toResponse(ticketRepository.saveAndFlush(ticket));
    }

    @Transactional(readOnly = true)
    public TicketResponse getById(UUID id) {
        return ticketMapper.toResponse(getTicket(id));
    }

    @Transactional(readOnly = true)
    public PageResponse<TicketSummaryResponse> list(
            String keyword, TicketStatus status, Pageable pageable) {
        Specification<Ticket> specification = Specification.where(null);

        if (keyword != null && !keyword.isBlank()) {
            String pattern = "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";
            specification = specification.and((root, query, builder) -> builder.or(
                    builder.like(builder.lower(root.get("title")), pattern),
                    builder.like(builder.lower(root.get("description")), pattern)));
        }
        if (status != null) {
            specification = specification.and(
                    (root, query, builder) -> builder.equal(root.get("status"), status));
        }

        Page<TicketSummaryResponse> page =
                ticketRepository.findAll(specification, pageable).map(ticketMapper::toSummary);
        return PageResponse.from(page);
    }

    @Transactional
    public TicketResponse update(UUID id, UpdateTicketRequest request) {
        Ticket ticket = getTicket(id);
        validateRequiredPatchFields(request);

        if (request.isTitlePresent()) {
            ticket.setTitle(request.getTitle());
        }
        if (request.isDescriptionPresent()) {
            ticket.setDescription(request.getDescription());
        }
        if (request.isPriorityPresent()) {
            ticket.setPriority(request.getPriority());
        }
        if (request.isAssigneePresent()) {
            ticket.setAssignee(request.getAssignee());
        }

        return ticketMapper.toResponse(ticketRepository.saveAndFlush(ticket));
    }

    @Transactional
    public CommentResponse addComment(UUID ticketId, AddCommentRequest request) {
        Ticket ticket = getTicket(ticketId);
        Comment comment = Comment.builder().ticket(ticket).body(request.body()).build();
        return ticketMapper.toCommentResponse(commentRepository.saveAndFlush(comment));
    }

    @Transactional
    public TicketResponse transition(UUID id, TicketStatus target) {
        Ticket ticket = getTicket(id);
        statusTransitionValidator.validate(ticket.getStatus(), target);
        ticket.setStatus(target);
        return ticketMapper.toResponse(ticketRepository.saveAndFlush(ticket));
    }

    private Ticket getTicket(UUID id) {
        return ticketRepository
                .findById(id)
                .orElseThrow(() -> new AppException(
                        ErrorCode.TICKET_NOT_FOUND, "Ticket " + id + " not found"));
    }

    private void validateRequiredPatchFields(UpdateTicketRequest request) {
        if ((request.isTitlePresent() && request.getTitle() == null)
                || (request.isDescriptionPresent() && request.getDescription() == null)
                || (request.isPriorityPresent() && request.getPriority() == null)) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Request validation failed");
        }
    }
}
