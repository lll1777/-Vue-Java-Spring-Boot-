package com.ticket.management.repository;

import com.ticket.management.entity.SLAWarning;
import com.ticket.management.enums.SLAStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SLAWarningRepository extends JpaRepository<SLAWarning, Long>, JpaSpecificationExecutor<SLAWarning> {

    List<SLAWarning> findByTicketIdOrderByTriggeredAtDesc(Long ticketId);

    List<SLAWarning> findByStatus(SLAStatus status);

    List<SLAWarning> findByStatusAndNotifiedFalse(SLAStatus status);
}
