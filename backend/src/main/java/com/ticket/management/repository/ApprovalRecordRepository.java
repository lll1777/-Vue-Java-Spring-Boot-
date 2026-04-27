package com.ticket.management.repository;

import com.ticket.management.entity.ApprovalRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApprovalRecordRepository extends JpaRepository<ApprovalRecord, Long>, JpaSpecificationExecutor<ApprovalRecord> {

    List<ApprovalRecord> findByInstanceIdOrderByCreatedAtDesc(Long instanceId);

    List<ApprovalRecord> findByApproverId(Long approverId);

    List<ApprovalRecord> findByInstanceId(Long instanceId);
}
