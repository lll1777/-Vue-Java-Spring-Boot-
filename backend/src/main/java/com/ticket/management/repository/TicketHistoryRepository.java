package com.ticket.management.repository;

import com.ticket.management.entity.TicketHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TicketHistoryRepository extends JpaRepository<TicketHistory, Long>, JpaSpecificationExecutor<TicketHistory> {

    List<TicketHistory> findByTicketIdOrderByActionTimeDesc(Long ticketId);

    List<TicketHistory> findByOperatorId(Long operatorId);

    List<TicketHistory> findByTicketId(Long ticketId);
}
