package com.ticket.management.security;

import com.ticket.management.entity.Department;
import com.ticket.management.entity.Ticket;
import com.ticket.management.entity.User;
import com.ticket.management.repository.DepartmentRepository;
import com.ticket.management.repository.TicketRepository;
import com.ticket.management.repository.UserRepository;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Aspect
@Component
public class TicketPermissionAspect {

    private static final Logger logger = LoggerFactory.getLogger(TicketPermissionAspect.class);

    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final PermissionEvaluatorImpl permissionEvaluator;

    private static final Set<String> ADMIN_ROLES = new HashSet<>(Arrays.asList(
        "ROLE_ADMIN",
        "ROLE_SUPER_ADMIN",
        "ROLE_SYSTEM_ADMIN"
    ));

    private static final Set<String> TICKET_OPERATE_PERMISSIONS = new HashSet<>(Arrays.asList(
        "TICKET_ASSIGN",
        "TICKET_EDIT_STATUS",
        "TICKET_RESOLVE",
        "TICKET_CLOSE",
        "TICKET_ESCALATE"
    ));

    public TicketPermissionAspect(
            TicketRepository ticketRepository,
            UserRepository userRepository,
            DepartmentRepository departmentRepository,
            PermissionEvaluatorImpl permissionEvaluator) {
        this.ticketRepository = ticketRepository;
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.permissionEvaluator = permissionEvaluator;
    }

    @Pointcut("execution(* com.ticket.management.service.TicketService.updateTicket(..))")
    public void updateTicketPointcut() {}

    @Pointcut("execution(* com.ticket.management.service.TicketService.assignTicket(..))")
    public void assignTicketPointcut() {}

    @Pointcut("execution(* com.ticket.management.service.TicketService.updateStatus(..))")
    public void updateStatusPointcut() {}

    @Pointcut("execution(* com.ticket.management.service.BatchProcessingService.batchAssign(..))")
    public void batchAssignPointcut() {}

    @Pointcut("execution(* com.ticket.management.service.BatchProcessingService.batchUpdateStatus(..))")
    public void batchUpdateStatusPointcut() {}

    @Pointcut("execution(* com.ticket.management.service.BatchProcessingService.batchClose(..))")
    public void batchClosePointcut() {}

    @Pointcut("execution(* com.ticket.management.service.BatchProcessingService.batchResolve(..))")
    public void batchResolvePointcut() {}

    @Around("updateTicketPointcut()")
    public Object checkUpdatePermission(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();
        Long ticketId = (Long) args[0];
        
        checkSingleTicketPermission(ticketId, "TICKET_EDIT");
        
        return joinPoint.proceed();
    }

    @Around("assignTicketPointcut()")
    public Object checkAssignPermission(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();
        Long ticketId = (Long) args[0];
        
        checkSingleTicketPermission(ticketId, "TICKET_ASSIGN");
        
        return joinPoint.proceed();
    }

    @Around("updateStatusPointcut()")
    public Object checkUpdateStatusPermission(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();
        Long ticketId = (Long) args[0];
        
        checkSingleTicketPermission(ticketId, "TICKET_EDIT_STATUS");
        
        return joinPoint.proceed();
    }

    @Around("batchAssignPointcut()")
    public Object checkBatchAssignPermission(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();
        @SuppressWarnings("unchecked")
        List<Long> ticketIds = (List<Long>) args[0];
        
        checkBatchTicketPermission(ticketIds, "TICKET_BATCH_ASSIGN");
        
        return joinPoint.proceed();
    }

    @Around("batchUpdateStatusPointcut() || batchClosePointcut() || batchResolvePointcut()")
    public Object checkBatchOperationPermission(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();
        @SuppressWarnings("unchecked")
        List<Long> ticketIds = (List<Long>) args[0];
        
        String methodName = joinPoint.getSignature().getName();
        String permissionCode;
        
        if ("batchAssign".equals(methodName)) {
            permissionCode = "TICKET_BATCH_ASSIGN";
        } else if ("batchClose".equals(methodName)) {
            permissionCode = "TICKET_BATCH_CLOSE";
        } else if ("batchResolve".equals(methodName)) {
            permissionCode = "TICKET_BATCH_RESOLVE";
        } else {
            permissionCode = "TICKET_BATCH_UPDATE_STATUS";
        }
        
        checkBatchTicketPermission(ticketIds, permissionCode);
        
        return joinPoint.proceed();
    }

    private void checkSingleTicketPermission(Long ticketId, String permissionCode) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new SecurityException("User not authenticated");
        }

        String username = authentication.getName();
        User currentUser = userRepository.findByUsername(username)
            .orElseThrow(() -> new SecurityException("User not found: " + username));

        if (isAdmin(currentUser)) {
            logger.debug("User {} is admin, bypassing department check for ticket {}", username, ticketId);
            return;
        }

        Ticket ticket = ticketRepository.findById(ticketId)
            .orElseThrow(() -> new IllegalArgumentException("Ticket not found: " + ticketId));

        if (!hasTicketPermission(currentUser, ticket, permissionCode)) {
            logger.warn("Permission denied for user {} to {} ticket {}", username, permissionCode, ticketId);
            throw new SecurityException(
                String.format("You don't have permission to %s this ticket", permissionCode)
            );
        }
    }

    private void checkBatchTicketPermission(List<Long> ticketIds, String permissionCode) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new SecurityException("User not authenticated");
        }

        String username = authentication.getName();
        User currentUser = userRepository.findByUsername(username)
            .orElseThrow(() -> new SecurityException("User not found: " + username));

        if (isAdmin(currentUser)) {
            logger.debug("User {} is admin, bypassing department check for batch operation", username);
            return;
        }

        for (Long ticketId : ticketIds) {
            Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found: " + ticketId));

            if (!hasTicketPermission(currentUser, ticket, permissionCode)) {
                logger.warn("Permission denied for user {} to {} ticket {} in batch operation", 
                    username, permissionCode, ticketId);
                throw new SecurityException(
                    String.format("You don't have permission to %s ticket: %s", 
                        permissionCode, ticket.getTicketNo())
                );
            }
        }
    }

    private boolean hasTicketPermission(User currentUser, Ticket ticket, String permissionCode) {
        Long currentUserId = currentUser.getId();
        Department currentUserDept = currentUser.getDepartment();
        Long currentUserDeptId = currentUserDept != null ? currentUserDept.getId() : null;

        Long ticketCreatorId = ticket.getCreator() != null ? ticket.getCreator().getId() : null;
        Long ticketAssigneeId = ticket.getAssignee() != null ? ticket.getAssignee().getId() : null;
        Long ticketDeptId = ticket.getDepartment() != null ? ticket.getDepartment().getId() : null;

        if (currentUserId.equals(ticketCreatorId)) {
            logger.debug("User {} is the creator of ticket {}", currentUserId, ticket.getId());
            return true;
        }

        if (currentUserId.equals(ticketAssigneeId)) {
            logger.debug("User {} is the assignee of ticket {}", currentUserId, ticket.getId());
            return true;
        }

        if (currentUserDeptId != null && currentUserDeptId.equals(ticketDeptId)) {
            logger.debug("User {} is in the same department as ticket {}", currentUserId, ticket.getId());
            return true;
        }

        if (isCollaborator(currentUser, ticket)) {
            logger.debug("User {} is a collaborator for ticket {}", currentUserId, ticket.getId());
            return true;
        }

        if (isSupervisor(currentUser, ticket)) {
            logger.debug("User {} is a supervisor of the assignee/department of ticket {}", 
                currentUserId, ticket.getId());
            return true;
        }

        return false;
    }

    private boolean isAdmin(User user) {
        return user.getRoles().stream()
            .anyMatch(role -> ADMIN_ROLES.contains("ROLE_" + role.getCode()) || 
                              ADMIN_ROLES.contains(role.getCode()));
    }

    private boolean isCollaborator(User user, Ticket ticket) {
        return false;
    }

    private boolean isSupervisor(User user, Ticket ticket) {
        User assignee = ticket.getAssignee();
        if (assignee == null) {
            return false;
        }

        Integer userLevel = user.getLevel();
        Integer assigneeLevel = assignee.getLevel();

        if (userLevel != null && assigneeLevel != null && userLevel > assigneeLevel) {
            Department userDept = user.getDepartment();
            Department assigneeDept = assignee.getDepartment();

            if (userDept != null && assigneeDept != null && 
                userDept.getId().equals(assigneeDept.getId())) {
                return true;
            }
        }

        return false;
    }

    public boolean canAccessTicket(User user, Ticket ticket) {
        if (isAdmin(user)) {
            return true;
        }

        Long userId = user.getId();
        Long creatorId = ticket.getCreator() != null ? ticket.getCreator().getId() : null;
        Long assigneeId = ticket.getAssignee() != null ? ticket.getAssignee().getId() : null;

        if (userId.equals(creatorId)) {
            return true;
        }

        if (userId.equals(assigneeId)) {
            return true;
        }

        Department userDept = user.getDepartment();
        Department ticketDept = ticket.getDepartment();

        if (userDept != null && ticketDept != null && 
            userDept.getId().equals(ticketDept.getId())) {
            return true;
        }

        return false;
    }

    public boolean canModifyTicket(User user, Ticket ticket) {
        if (!canAccessTicket(user, ticket)) {
            return false;
        }

        Long userId = user.getId();
        Long creatorId = ticket.getCreator() != null ? ticket.getCreator().getId() : null;
        Long assigneeId = ticket.getAssignee() != null ? ticket.getAssignee().getId() : null;

        if (userId.equals(creatorId)) {
            return true;
        }

        if (userId.equals(assigneeId)) {
            return true;
        }

        if (isAdmin(user)) {
            return true;
        }

        return false;
    }
}
