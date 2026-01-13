package br.com.luizbrand.worklog.tickets;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TicketLogRepository extends JpaRepository<TicketLog, Long> {
}
