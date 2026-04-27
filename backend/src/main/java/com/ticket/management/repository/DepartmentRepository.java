package com.ticket.management.repository;

import com.ticket.management.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long>, JpaSpecificationExecutor<Department> {

    Optional<Department> findByCode(String code);

    List<Department> findByParentId(Long parentId);

    List<Department> findByEnabledTrue();

    boolean existsByCode(String code);

    @Query("SELECT d FROM Department d LEFT JOIN FETCH d.collaborators WHERE d.id = :id")
    Optional<Department> findByIdWithCollaborators(@Param("id") Long id);
}
