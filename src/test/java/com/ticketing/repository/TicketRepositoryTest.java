package com.ticketing.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ticketing.domain.Comment;
import com.ticketing.domain.Priority;
import com.ticketing.domain.Ticket;
import com.ticketing.domain.TicketStatus;
import com.ticketing.support.PostgresIntegrationTest;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Tag("integration")
class TicketRepositoryTest extends PostgresIntegrationTest {

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void shouldPersistTicketAndComment() {
        Ticket ticket = Ticket.builder()
                .title("Cannot reset password")
                .description("Reset email never arrives.")
                .priority(Priority.HIGH)
                .assignee("Alex Chen")
                .build();
        ticket.addComment(Comment.builder().body("Asked the user for screenshots.").build());

        Ticket saved = ticketRepository.saveAndFlush(ticket);
        entityManager.clear();

        Ticket loaded = ticketRepository.findById(saved.getId()).orElseThrow();
        assertThat(loaded.getStatus()).isEqualTo(TicketStatus.OPEN);
        assertThat(loaded.getPriority()).isEqualTo(Priority.HIGH);
        assertThat(loaded.getComments()).hasSize(1);
        assertThat(loaded.getComments().getFirst().getBody()).isEqualTo("Asked the user for screenshots.");
        assertThat(loaded.getCreatedAt()).isNotNull();
        assertThat(loaded.getUpdatedAt()).isNotNull();
    }

    @Test
    void shouldRejectInvalidStatusAtDatabase() {
        UUID id = UUID.randomUUID();
        Instant now = Instant.parse("2026-09-24T05:00:00Z");

        assertThatThrownBy(() -> {
                    entityManager
                            .createNativeQuery(
                                    """
                                    INSERT INTO ticket
                                      (id, title, description, status, priority, assignee, created_at, updated_at)
                                    VALUES
                                      (:id, :title, :description, :status, :priority, :assignee, :createdAt, :updatedAt)
                                    """)
                            .setParameter("id", id)
                            .setParameter("title", "Bad status")
                            .setParameter("description", "Should fail CHECK")
                            .setParameter("status", "NOPE")
                            .setParameter("priority", "HIGH")
                            .setParameter("assignee", null)
                            .setParameter("createdAt", now)
                            .setParameter("updatedAt", now)
                            .executeUpdate();
                    entityManager.flush();
                })
                .isInstanceOfAny(
                        DataIntegrityViolationException.class, ConstraintViolationException.class);
    }
}
