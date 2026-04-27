package com.ticket.management.repository;

import com.ticket.management.entity.ApprovalNode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApprovalNodeRepository extends JpaRepository<ApprovalNode, Long>, JpaSpecificationExecutor<ApprovalNode> {

    List<ApprovalNode> findByFlowIdOrderByOrderIndex(Long flowId);

    List<ApprovalNode> findByFlowId(Long flowId);
}
