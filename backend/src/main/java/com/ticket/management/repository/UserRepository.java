package com.ticket.management.repository;

import com.ticket.management.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    List<User> findByDepartmentId(Long departmentId);

    List<User> findByActiveTrue();

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    List<User> findByRolesCodeIn(List<String> roleCodes);

    List<User> findByLevelGreaterThanEqual(Integer level);
}
