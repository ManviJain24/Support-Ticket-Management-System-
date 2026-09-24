package com.ticketing.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ticketing.domain.Priority;
import com.ticketing.domain.Ticket;
import com.ticketing.domain.TicketStatus;
import com.ticketing.repository.TicketRepository;
import com.ticketing.support.PostgresIntegrationTest;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Stream;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Tag("integration")
class TicketStateMachineIntegrationTest extends PostgresIntegrationTest {

    private static final Set<Transition> VALID_TRANSITIONS = Set.of(
            new Transition(TicketStatus.OPEN, TicketStatus.IN_PROGRESS),
            new Transition(TicketStatus.OPEN, TicketStatus.CANCELLED),
            new Transition(TicketStatus.IN_PROGRESS, TicketStatus.RESOLVED),
            new Transition(TicketStatus.IN_PROGRESS, TicketStatus.CANCELLED),
            new Transition(TicketStatus.RESOLVED, TicketStatus.CLOSED));

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TicketRepository ticketRepository;

    @BeforeEach
    void cleanDatabase() {
        ticketRepository.deleteAll();
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @MethodSource("validTransitions")
    void shouldPersistTransition_whenPairIsValid(TicketStatus from, TicketStatus to)
            throws Exception {
        Ticket ticket = ticketRepository.saveAndFlush(ticketAt(from));

        mockMvc.perform(post("/api/v1/tickets/{id}/status", ticket.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(statusBody(to)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(to.name()));

        mockMvc.perform(get("/api/v1/tickets/{id}", ticket.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(to.name()));
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @MethodSource("invalidTransitions")
    void shouldReturn422AndKeepStatus_whenPairIsInvalid(TicketStatus from, TicketStatus to)
            throws Exception {
        Ticket ticket = ticketRepository.saveAndFlush(ticketAt(from));

        mockMvc.perform(post("/api/v1/tickets/{id}/status", ticket.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(statusBody(to)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("INVALID_TRANSITION"));

        mockMvc.perform(get("/api/v1/tickets/{id}", ticket.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(from.name()));
    }

    @Test
    void shouldReturn400_whenStatusIsMissing() throws Exception {
        Ticket ticket = ticketRepository.saveAndFlush(ticketAt(TicketStatus.OPEN));

        mockMvc.perform(post("/api/v1/tickets/{id}/status", ticket.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void shouldReturn400_whenStatusIsUnknown() throws Exception {
        Ticket ticket = ticketRepository.saveAndFlush(ticketAt(TicketStatus.OPEN));

        mockMvc.perform(post("/api/v1/tickets/{id}/status", ticket.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"NOPE\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void shouldReturn404_whenTicketDoesNotExist() throws Exception {
        mockMvc.perform(post("/api/v1/tickets/{id}/status", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(statusBody(TicketStatus.IN_PROGRESS)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TICKET_NOT_FOUND"));
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

    private String statusBody(TicketStatus status) {
        return "{\"status\":\"" + status.name() + "\"}";
    }

    private Ticket ticketAt(TicketStatus status) {
        return Ticket.builder()
                .title("Ticket")
                .description("Description")
                .priority(Priority.MEDIUM)
                .status(status)
                .build();
    }

    private record Transition(TicketStatus from, TicketStatus to) {}
}
