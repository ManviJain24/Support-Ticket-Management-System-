package com.ticketing.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ticketing.domain.Priority;
import com.ticketing.domain.Ticket;
import com.ticketing.domain.TicketStatus;
import com.ticketing.domain.TicketStatusTransitionValidator;
import com.ticketing.dto.request.AddCommentRequest;
import com.ticketing.dto.request.CreateTicketRequest;
import com.ticketing.dto.request.UpdateTicketRequest;
import com.ticketing.exception.AppException;
import com.ticketing.exception.ErrorCode;
import com.ticketing.mapper.TicketMapper;
import com.ticketing.repository.CommentRepository;
import com.ticketing.repository.TicketRepository;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class TicketServiceTest {

    private static final Set<Transition> VALID_TRANSITIONS = Set.of(
            new Transition(TicketStatus.OPEN, TicketStatus.IN_PROGRESS),
            new Transition(TicketStatus.OPEN, TicketStatus.CANCELLED),
            new Transition(TicketStatus.IN_PROGRESS, TicketStatus.RESOLVED),
            new Transition(TicketStatus.IN_PROGRESS, TicketStatus.CANCELLED),
            new Transition(TicketStatus.RESOLVED, TicketStatus.CLOSED));

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private CommentRepository commentRepository;

    private TicketService ticketService;

    @BeforeEach
    void setUp() {
        ticketService = new TicketService(
                ticketRepository,
                commentRepository,
                new TicketMapper(),
                new TicketStatusTransitionValidator());
    }

    @Test
    void shouldCreateOpenTicket_whenRequestIsValid() {
        when(ticketRepository.saveAndFlush(any(Ticket.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = ticketService.create(
                new CreateTicketRequest("Printer offline", "Third floor", Priority.HIGH, null));

        ArgumentCaptor<Ticket> captor = ArgumentCaptor.forClass(Ticket.class);
        verify(ticketRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(TicketStatus.OPEN);
        assertThat(response.status()).isEqualTo(TicketStatus.OPEN);
    }

    @Test
    void shouldUpdateOnlyPresentFields_whenPatchIsPartial() {
        Ticket ticket = ticketAt(TicketStatus.OPEN);
        String originalDescription = ticket.getDescription();
        UpdateTicketRequest request = new UpdateTicketRequest();
        request.setAssignee("Sam Lee");
        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));
        when(ticketRepository.saveAndFlush(ticket)).thenReturn(ticket);

        var response = ticketService.update(ticket.getId(), request);

        assertThat(response.assignee()).isEqualTo("Sam Lee");
        assertThat(response.description()).isEqualTo(originalDescription);
    }

    @Test
    void shouldNotSaveComment_whenTicketIsMissing() {
        UUID id = UUID.randomUUID();
        when(ticketRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ticketService.addComment(id, new AddCommentRequest("Comment")))
                .isInstanceOfSatisfying(AppException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.TICKET_NOT_FOUND));
        verify(commentRepository, never()).saveAndFlush(any());
    }

    @Test
    void shouldApplySearchAndStatusSpecifications_whenListingTickets() {
        PageRequest pageable = PageRequest.of(0, 20);
        when(ticketRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(Page.empty(pageable));

        var response = ticketService.list("printer", TicketStatus.OPEN, pageable);

        assertThat(response.content()).isEmpty();
        verify(ticketRepository).findAll(any(Specification.class), eq(pageable));
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @MethodSource("validTransitions")
    void shouldTransitionAndSave_whenPairIsValid(TicketStatus from, TicketStatus to) {
        Ticket ticket = ticketAt(from);
        when(ticketRepository.findById(ticket.getId())).thenReturn(java.util.Optional.of(ticket));
        when(ticketRepository.saveAndFlush(ticket)).thenReturn(ticket);

        var response = ticketService.transition(ticket.getId(), to);

        assertThat(response.status()).isEqualTo(to);
        verify(ticketRepository).saveAndFlush(ticket);
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @MethodSource("invalidTransitions")
    void shouldRejectWithoutSaving_whenPairIsInvalid(TicketStatus from, TicketStatus to) {
        Ticket ticket = ticketAt(from);
        when(ticketRepository.findById(ticket.getId())).thenReturn(java.util.Optional.of(ticket));

        assertThatThrownBy(() -> ticketService.transition(ticket.getId(), to))
                .isInstanceOfSatisfying(AppException.class, exception -> {
                    assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_TRANSITION);
                    assertThat(exception.getMessage())
                            .isEqualTo("Cannot transition ticket from " + from + " to " + to);
                });
        verify(ticketRepository, never()).saveAndFlush(any());
    }

    @Test
    void shouldReject_whenTicketIsMissing() {
        UUID id = UUID.randomUUID();
        when(ticketRepository.findById(id)).thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> ticketService.transition(id, TicketStatus.IN_PROGRESS))
                .isInstanceOfSatisfying(AppException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.TICKET_NOT_FOUND));
        verify(ticketRepository, never()).saveAndFlush(any());
    }

    private static Stream<Arguments> validTransitions() {
        return VALID_TRANSITIONS.stream()
                .map(transition -> Arguments.of(transition.from(), transition.to()));
    }

    private static Stream<Arguments> invalidTransitions() {
        return Arrays.stream(TicketStatus.values())
                .flatMap(from -> Arrays.stream(TicketStatus.values())
                        .map(to -> new Transition(from, to)))
                .filter(transition -> !VALID_TRANSITIONS.contains(transition))
                .map(transition -> Arguments.of(transition.from(), transition.to()));
    }

    private Ticket ticketAt(TicketStatus status) {
        return Ticket.builder()
                .id(UUID.randomUUID())
                .title("Ticket")
                .description("Description")
                .priority(Priority.MEDIUM)
                .status(status)
                .build();
    }

    private record Transition(TicketStatus from, TicketStatus to) {}
}
