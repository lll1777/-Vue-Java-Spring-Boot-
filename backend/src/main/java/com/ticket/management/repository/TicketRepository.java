package com.ticket.management.repository;

import com.ticket.management.entity.Ticket;
import com.ticket.management.enums.TicketStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import javax.persistence.LockModeType;
import javax.persistence.QueryHint;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long>, JpaSpecificationExecutor<Ticket> {

    Optional<Ticket> findByTicketNo(String ticketNo);

    Page<Ticket> findByCreatorId(Long creatorId, Pageable pageable);

    Page<Ticket> findByAssigneeId(Long assigneeId, Pageable pageable);

    Page<Ticket> findByDepartmentId(Long departmentId, Pageable pageable);

    Page<Ticket> findByStatus(TicketStatus status, Pageable pageable);

    List<Ticket> findByStatusIn(List<TicketStatus> statuses);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({@QueryHint(name = "javax.persistence.lock.timeout", value = "3000")})
    @Query("SELECT t FROM Ticket t WHERE t.id = :id")
    Optional<Ticket> findByIdWithLock(@Param("id") Long id);

    @Query("SELECT t FROM Ticket t WHERE t.status IN :statuses AND t.slaDeadline IS NOT NULL " +
           "AND t.slaDeadline <= :warningTime AND t.slaWarningSent = false")
    List<Ticket> findTicketsForSlaWarning(
        @Param("statuses") List<TicketStatus> statuses,
        @Param("warningTime") LocalDateTime warningTime
    );

    @Query("SELECT t FROM Ticket t WHERE t.status IN :statuses AND t.slaDeadline IS NOT NULL " +
           "AND t.slaDeadline <= :now AND t.slaOverdue = false")
    List<Ticket> findOverdueTickets(
        @Param("statuses") List<TicketStatus> statuses,
        @Param("now") LocalDateTime now
    );

    @Query("SELECT COUNT(t) FROM Ticket t WHERE t.assignee.id = :assigneeId AND t.status IN :statuses")
    long countByAssigneeIdAndStatusIn(
        @Param("assigneeId") Long assigneeId,
        @Param("statuses") List<TicketStatus> statuses
    );

    @Query("SELECT COUNT(t) FROM Ticket t WHERE t.department.id = :departmentId AND t.status = :status")
    long countByDepartmentIdAndStatus(
        @Param("departmentId") Long departmentId,
        @Param("status") TicketStatus status
    );

    @Query("SELECT t FROM Ticket t WHERE t.createdAt BETWEEN :startDate AND :endDate")
    List<Ticket> findByCreatedAtBetween(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    boolean existsByTicketNo(String ticketNo);
}
