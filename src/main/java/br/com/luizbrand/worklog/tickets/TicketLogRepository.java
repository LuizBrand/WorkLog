package br.com.luizbrand.worklog.tickets;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface TicketLogRepository extends JpaRepository<TicketLog, Long> {

    Page<TicketLog> findByTicket_PublicIdOrderByChangeDateDesc(UUID ticketPublicId, Pageable pageable);

}
