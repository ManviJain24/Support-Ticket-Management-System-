package com.ticketing.mapper;

import com.ticketing.domain.Comment;
import com.ticketing.domain.Ticket;
import com.ticketing.dto.response.CommentResponse;
import com.ticketing.dto.response.TicketResponse;
import com.ticketing.dto.response.TicketSummaryResponse;
import org.springframework.stereotype.Component;

@Component
public class TicketMapper {

    public TicketSummaryResponse toSummary(Ticket ticket) {
        return new TicketSummaryResponse(
                ticket.getId(),
                ticket.getTitle(),
                ticket.getDescription(),
                ticket.getStatus(),
                ticket.getPriority(),
                ticket.getAssignee(),
                ticket.getCreatedAt(),
                ticket.getUpdatedAt());
    }

    public TicketResponse toResponse(Ticket ticket) {
        return new TicketResponse(
                ticket.getId(),
                ticket.getTitle(),
                ticket.getDescription(),
                ticket.getStatus(),
                ticket.getPriority(),
                ticket.getAssignee(),
                ticket.getCreatedAt(),
                ticket.getUpdatedAt(),
                ticket.getComments().stream().map(this::toCommentResponse).toList());
    }

    public CommentResponse toCommentResponse(Comment comment) {
        return new CommentResponse(comment.getId(), comment.getBody(), comment.getCreatedAt());
    }
}
