package com.ticket.management.repository;

import com.ticket.management.entity.ReportConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReportConfigRepository extends JpaRepository<ReportConfig, Long>, JpaSpecificationExecutor<ReportConfig> {

    Optional<ReportConfig> findByCode(String code);

    List<ReportConfig> findByIsSystemTrueAndEnabledTrue();

    List<ReportConfig> findByOwnerIdOrIsPublicTrue(Long ownerId);

    List<ReportConfig> findByDepartmentIdOrIsPublicTrue(Long departmentId);

    boolean existsByCode(String code);
}
