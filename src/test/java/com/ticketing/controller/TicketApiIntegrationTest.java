package com.ticketing.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketing.domain.Priority;
import com.ticketing.domain.Ticket;
import com.ticketing.domain.TicketStatus;
import com.ticketing.repository.TicketRepository;
import com.ticketing.support.PostgresIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Tag("integration")
class TicketApiIntegrationTest extends PostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TicketRepository ticketRepository;

    @BeforeEach
    void cleanDatabase() {
        ticketRepository.deleteAll();
    }

    @Test
    void shouldCreateGetUpdateAndCommentOnTicket() throws Exception {
        String createBody =
                """
                {
                  "title": "Cannot reset password",
                  "description": "Reset email never arrives.",
                  "priority": "HIGH",
                  "assignee": "Alex Chen"
                }
                """;

        String response = mockMvc.perform(post("/api/v1/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("/api/v1/tickets/")))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.comments", hasSize(0)))
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode created = objectMapper.readTree(response);
        String id = created.get("id").asText();

        mockMvc.perform(patch("/api/v1/tickets/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"priority\":\"MEDIUM\",\"assignee\":null}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.priority").value("MEDIUM"))
                .andExpect(jsonPath("$.description").value("Reset email never arrives."))
                .andExpect(jsonPath("$.assignee").value(nullValue()));

        mockMvc.perform(post("/api/v1/tickets/{id}/comments", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"body\":\"Asked for screenshots.\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.body").value("Asked for screenshots."));

        mockMvc.perform(get("/api/v1/tickets/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Reset email never arrives."))
                .andExpect(jsonPath("$.comments", hasSize(1)));
    }

    @Test
    void shouldListWithoutCommentsAndReturnPaginationMetadata() throws Exception {
        ticketRepository.saveAndFlush(ticket("First", "Description", TicketStatus.OPEN));
        ticketRepository.saveAndFlush(ticket("Second", "Description", TicketStatus.RESOLVED));

        mockMvc.perform(get("/api/v1/tickets").param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].comments").doesNotExist())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void shouldSearchAndFilterTickets() throws Exception {
        ticketRepository.saveAndFlush(ticket("Printer offline", "Third floor", TicketStatus.OPEN));
        ticketRepository.saveAndFlush(
                ticket("Password reset", "Email missing", TicketStatus.RESOLVED));

        mockMvc.perform(get("/api/v1/tickets").param("q", "printer"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].title").value("Printer offline"));

        mockMvc.perform(get("/api/v1/tickets").param("q", "email"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].title").value("Password reset"));

        mockMvc.perform(get("/api/v1/tickets").param("status", "RESOLVED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].status").value("RESOLVED"));

        mockMvc.perform(get("/api/v1/tickets")
                        .param("q", "printer")
                        .param("status", "RESOLVED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)));

        mockMvc.perform(get("/api/v1/tickets").param("status", "NOPE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void shouldRejectBlankCommentAndUnknownTicket() throws Exception {
        Ticket ticket =
                ticketRepository.saveAndFlush(ticket("Ticket", "Description", TicketStatus.OPEN));

        mockMvc.perform(post("/api/v1/tickets/{id}/comments", ticket.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"body\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

        mockMvc.perform(post("/api/v1/tickets/{id}/comments", java.util.UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"body\":\"Comment\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TICKET_NOT_FOUND"));
    }

    @Test
    void shouldReturnValidationErrors() throws Exception {
        mockMvc.perform(post("/api/v1/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"\",\"description\":\"\",\"priority\":null}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.details").isArray());

        Ticket ticket =
                ticketRepository.saveAndFlush(ticket("Ticket", "Description", TicketStatus.OPEN));
        mockMvc.perform(patch("/api/v1/tickets/{id}", ticket.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CLOSED\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void shouldRejectInvalidPaginationAndReturn404() throws Exception {
        mockMvc.perform(get("/api/v1/tickets").param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

        mockMvc.perform(get("/api/v1/tickets").param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

        mockMvc.perform(get("/api/v1/tickets/{id}", java.util.UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TICKET_NOT_FOUND"));

        mockMvc.perform(patch("/api/v1/tickets/{id}", java.util.UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"assignee\":\"Sam Lee\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TICKET_NOT_FOUND"));
    }

    private Ticket ticket(String title, String description, TicketStatus status) {
        return Ticket.builder()
                .title(title)
                .description(description)
                .priority(Priority.MEDIUM)
                .status(status)
                .build();
    }
}
