package com.ticketing.repository;

import com.ticketing.domain.Ticket;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface TicketRepository
        extends JpaRepository<Ticket, UUID>, JpaSpecificationExecutor<Ticket> {}
