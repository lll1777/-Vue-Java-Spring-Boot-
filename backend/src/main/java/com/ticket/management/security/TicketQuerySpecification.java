package com.ticket.management.security;

import com.ticket.management.entity.Ticket;
import com.ticket.management.entity.Ticket_;
import com.ticket.management.entity.User;
import com.ticket.management.entity.User_;
import com.ticket.management.entity.Department;
import com.ticket.management.entity.Department_;
import com.ticket.management.security.DataPermissionService.DataPermissionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import javax.persistence.criteria.*;

@Component
public class TicketQuerySpecification {

    private static final Logger logger = LoggerFactory.getLogger(TicketQuerySpecification.class);

    private final DataPermissionService dataPermissionService;

    public TicketQuerySpecification(DataPermissionService dataPermissionService) {
        this.dataPermissionService = dataPermissionService;
    }

    public Specification<Ticket> withDataPermission() {
        return (root, query, criteriaBuilder) -> {
            DataPermissionContext context = dataPermissionService.getCurrentUserPermissionContext();
            
            if (context.isAdmin()) {
                logger.debug("User is admin, bypassing data permission filter");
                return criteriaBuilder.conjunction();
            }
            
            return buildDataPermissionPredicate(root, criteriaBuilder, context);
        };
    }

    public Specification<Ticket> withDataPermissionAndUserId(Long userId) {
        return (root, query, criteriaBuilder) -> {
            DataPermissionContext context = dataPermissionService.getCurrentUserPermissionContext();
            
            if (context.isAdmin()) {
                return criteriaBuilder.equal(
                    root.get(Ticket_.creator).get(User_.id), 
                    userId
                );
            }
            
            Predicate dataPermission = buildDataPermissionPredicate(root, criteriaBuilder, context);
            Predicate userIdPredicate = criteriaBuilder.equal(
                root.get(Ticket_.creator).get(User_.id), 
                userId
            );
            
            return criteriaBuilder.and(dataPermission, userIdPredicate);
        };
    }

    public Specification<Ticket> withDataPermissionAndAssignee(Long assigneeId) {
        return (root, query, criteriaBuilder) -> {
            DataPermissionContext context = dataPermissionService.getCurrentUserPermissionContext();
            
            if (context.isAdmin()) {
                return criteriaBuilder.equal(
                    root.get(Ticket_.assignee).get(User_.id), 
                    assigneeId
                );
            }
            
            Predicate dataPermission = buildDataPermissionPredicate(root, criteriaBuilder, context);
            Predicate assigneePredicate = criteriaBuilder.equal(
                root.get(Ticket_.assignee).get(User_.id), 
                assigneeId
            );
            
            return criteriaBuilder.and(dataPermission, assigneePredicate);
        };
    }

    public Specification<Ticket> withDataPermissionAndDepartment(Long departmentId) {
        return (root, query, criteriaBuilder) -> {
            DataPermissionContext context = dataPermissionService.getCurrentUserPermissionContext();
            
            if (context.isAdmin()) {
                return criteriaBuilder.equal(
                    root.get(Ticket_.department).get(Department_.id), 
                    departmentId
                );
            }
            
            Predicate dataPermission = buildDataPermissionPredicate(root, criteriaBuilder, context);
            Predicate deptPredicate = criteriaBuilder.equal(
                root.get(Ticket_.department).get(Department_.id), 
                departmentId
            );
            
            return criteriaBuilder.and(dataPermission, deptPredicate);
        };
    }

    public Specification<Ticket> withCreatorAndAssigneeFilter(Long creatorId, Long assigneeId) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new java.util.ArrayList<>();
            
            DataPermissionContext context = dataPermissionService.getCurrentUserPermissionContext();
            if (!context.isAdmin()) {
                predicates.add(buildDataPermissionPredicate(root, criteriaBuilder, context));
            }
            
            if (creatorId != null) {
                predicates.add(criteriaBuilder.equal(
                    root.get(Ticket_.creator).get(User_.id), 
                    creatorId
                ));
            }
            
            if (assigneeId != null) {
                predicates.add(criteriaBuilder.equal(
                    root.get(Ticket_.assignee).get(User_.id), 
                    assigneeId
                ));
            }
            
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    private Predicate buildDataPermissionPredicate(
            Root<Ticket> root, 
            CriteriaBuilder criteriaBuilder, 
            DataPermissionContext context) {
        
        Long userId = context.getUserId();
        Long departmentId = context.getDepartmentId();
        
        List<Predicate> orPredicates = new java.util.ArrayList<>();
        
        Predicate isCreator = criteriaBuilder.equal(
            root.get(Ticket_.creator).get(User_.id), 
            userId
        );
        orPredicates.add(isCreator);
        
        Predicate isAssignee = criteriaBuilder.equal(
            root.get(Ticket_.assignee).get(User_.id), 
            userId
        );
        orPredicates.add(isAssignee);
        
        if (departmentId != null) {
            Predicate inSameDepartment = criteriaBuilder.equal(
                root.get(Ticket_.department).get(Department_.id), 
                departmentId
            );
            orPredicates.add(inSameDepartment);
        }
        
        Predicate combinedPredicate = criteriaBuilder.or(orPredicates.toArray(new Predicate[0]));
        
        logger.debug("Built data permission predicate for user {}: {}", userId, combinedPredicate);
        
        return combinedPredicate;
    }

    public interface HasDataPermission {
    }

    public static class TicketPermissionException extends RuntimeException {
        public TicketPermissionException(String message) {
            super(message);
        }
    }
}
