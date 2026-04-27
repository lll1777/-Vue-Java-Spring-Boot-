package com.ticket.management.repository;

import com.ticket.management.entity.ApprovalFlow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApprovalFlowRepository extends JpaRepository<ApprovalFlow, Long>, JpaSpecificationExecutor<ApprovalFlow> {

    Optional<ApprovalFlow> findByCode(String code);

    List<ApprovalFlow> findByEnabledTrue();

    List<ApprovalFlow> findByFlowTypeAndEnabledTrue(String flowType);

    @Query("SELECT f FROM ApprovalFlow f WHERE f.flowType = :flowType " +
           "AND f.enabled = true " +
           "AND (f.triggerCategory.id = :categoryId OR f.triggerCategory IS NULL) " +
           "AND (f.triggerPriorityFrom IS NULL OR f.triggerPriorityFrom <= :priority) " +
           "AND (f.triggerPriorityTo IS NULL OR f.triggerPriorityTo >= :priority) " +
           "AND (f.triggerDepartmentId IS NULL OR f.triggerDepartmentId = :departmentId) " +
           "ORDER BY f.isDefault DESC, f.createdAt ASC")
    List<ApprovalFlow> findMatchingFlows(
        @Param("flowType") String flowType,
        @Param("categoryId") Long categoryId,
        @Param("priority") Integer priority,
        @Param("departmentId") Long departmentId
    );

    @Query("SELECT f FROM ApprovalFlow f WHERE f.isDefault = true AND f.flowType = :flowType AND f.enabled = true")
    Optional<ApprovalFlow> findDefaultFlow(@Param("flowType") String flowType);

    boolean existsByCode(String code);
}
