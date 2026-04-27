package com.ticket.management.repository;

import com.ticket.management.entity.SLARule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SLARuleRepository extends JpaRepository<SLARule, Long>, JpaSpecificationExecutor<SLARule> {

    Optional<SLARule> findByCode(String code);

    List<SLARule> findByEnabledTrue();

    Optional<SLARule> findByIsDefaultTrueAndEnabledTrue();

    @Query("SELECT s FROM SLARule s WHERE s.enabled = true " +
           "AND (s.category.id = :categoryId OR s.category IS NULL) " +
           "AND (s.priorityLevel = :priorityLevel OR s.priorityLevel IS NULL) " +
           "ORDER BY (CASE WHEN s.category IS NOT NULL THEN 0 ELSE 1 END), " +
           "(CASE WHEN s.priorityLevel IS NOT NULL THEN 0 ELSE 1 END)")
    List<SLARule> findMatchingRules(
        @Param("categoryId") Long categoryId,
        @Param("priorityLevel") Integer priorityLevel
    );

    boolean existsByCode(String code);
}
