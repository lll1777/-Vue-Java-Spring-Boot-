package com.ticket.management.repository;

import com.ticket.management.entity.TicketCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TicketCategoryRepository extends JpaRepository<TicketCategory, Long>, JpaSpecificationExecutor<TicketCategory> {

    Optional<TicketCategory> findByCode(String code);

    List<TicketCategory> findByParentId(Long parentId);

    List<TicketCategory> findByEnabledTrue();

    boolean existsByCode(String code);
}
