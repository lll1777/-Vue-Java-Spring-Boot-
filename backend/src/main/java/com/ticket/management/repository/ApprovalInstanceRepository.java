package com.ticket.management.repository;

import com.ticket.management.entity.ApprovalInstance;
import com.ticket.management.enums.ApprovalStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import javax.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

@Repository
public interface ApprovalInstanceRepository extends JpaRepository<ApprovalInstance, Long>, JpaSpecificationExecutor<ApprovalInstance> {

    Optional<ApprovalInstance> findByInstanceNo(String instanceNo);

    List<ApprovalInstance> findByTicketId(Long ticketId);

    Page<ApprovalInstance> findByInitiatorId(Long initiatorId, Pageable pageable);

    Page<ApprovalInstance> findByStatus(ApprovalStatus status, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM ApprovalInstance a WHERE a.id = :id")
    Optional<ApprovalInstance> findByIdWithLock(@Param("id") Long id);

    @Query("SELECT a FROM ApprovalInstance a WHERE a.status = :status " +
           "AND a.currentNode.timeoutMinutes IS NOT NULL " +
           "AND a.currentNode.autoApproveOnTimeout = true " +
           "AND a.startedAt <= :timeoutTime")
    List<ApprovalInstance> findTimeoutInstancesForAutoApprove(
        @Param("status") ApprovalStatus status,
        @Param("timeoutTime") java.time.LocalDateTime timeoutTime
    );

    Optional<ApprovalInstance> findByTicketIdAndStatus(Long ticketId, ApprovalStatus status);

    List<ApprovalInstance> findByTicketIdOrderByCreatedAtDesc(Long ticketId);
}
