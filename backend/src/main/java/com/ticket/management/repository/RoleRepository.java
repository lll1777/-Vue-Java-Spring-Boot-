package com.ticket.management.repository;

import com.ticket.management.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long>, JpaSpecificationExecutor<Role> {

    Optional<Role> findByCode(String code);

    List<Role> findByLevelLessThanEqual(Integer level);

    List<Role> findByLevelGreaterThanEqual(Integer level);

    boolean existsByCode(String code);
}
