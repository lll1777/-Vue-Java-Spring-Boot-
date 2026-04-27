package com.ticket.management.repository;

import com.ticket.management.entity.ApprovalSignNode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApprovalSignNodeRepository extends JpaRepository<ApprovalSignNode, Long>, JpaSpecificationExecutor<ApprovalSignNode> {

    List<ApprovalSignNode> findByInstanceId(Long instanceId);

    Optional<ApprovalSignNode> findByInstanceIdAndActiveTrue(Long instanceId);

    List<ApprovalSignNode> findByInstanceIdOrderBySortOrderAsc(Long instanceId);

    List<ApprovalSignNode> findByInstanceIdAndActiveTrueOrderBySortOrderAsc(Long instanceId);
}
