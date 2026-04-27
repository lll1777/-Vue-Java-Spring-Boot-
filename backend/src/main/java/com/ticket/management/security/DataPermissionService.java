package com.ticket.management.security;

import com.ticket.management.entity.Department;
import com.ticket.management.entity.Ticket;
import com.ticket.management.entity.User;
import com.ticket.management.repository.DepartmentRepository;
import com.ticket.management.repository.TicketRepository;
import com.ticket.management.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class DataPermissionService {

    private static final Logger logger = LoggerFactory.getLogger(DataPermissionService.class);

    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;

    private static final Set<String> ADMIN_ROLES = new HashSet<>(Arrays.asList(
        "ROLE_ADMIN",
        "ROLE_SUPER_ADMIN",
        "ROLE_SYSTEM_ADMIN"
    ));

    @Autowired
    public DataPermissionService(
            TicketRepository ticketRepository,
            UserRepository userRepository,
            DepartmentRepository departmentRepository) {
        this.ticketRepository = ticketRepository;
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
    }

    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new SecurityException("用户未认证");
        }
        
        String username = authentication.getName();
        return userRepository.findByUsername(username)
            .orElseThrow(() -> new SecurityException("用户不存在: " + username));
    }

    public Long getCurrentUserId() {
        return getCurrentUser().getId();
    }

    public Long getCurrentUserDepartmentId() {
        User user = getCurrentUser();
        Department dept = user.getDepartment();
        return dept != null ? dept.getId() : null;
    }

    public boolean isCurrentUserAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        
        return authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .anyMatch(ADMIN_ROLES::contains);
    }

    public boolean canViewTicket(Ticket ticket) {
        if (ticket == null) {
            return false;
        }
        
        if (isCurrentUserAdmin()) {
            return true;
        }
        
        User currentUser = getCurrentUser();
        return canViewTicket(currentUser, ticket);
    }

    public boolean canViewTicket(User currentUser, Ticket ticket) {
        Long currentUserId = currentUser.getId();
        Department currentUserDept = currentUser.getDepartment();
        Long currentUserDeptId = currentUserDept != null ? currentUserDept.getId() : null;

        Long ticketCreatorId = ticket.getCreator() != null ? ticket.getCreator().getId() : null;
        Long ticketAssigneeId = ticket.getAssignee() != null ? ticket.getAssignee().getId() : null;
        Long ticketDeptId = ticket.getDepartment() != null ? ticket.getDepartment().getId() : null;

        if (currentUserId.equals(ticketCreatorId)) {
            logger.debug("User {} is creator of ticket {}", currentUserId, ticket.getId());
            return true;
        }

        if (currentUserId.equals(ticketAssigneeId)) {
            logger.debug("User {} is assignee of ticket {}", currentUserId, ticket.getId());
            return true;
        }

        if (currentUserDeptId != null && currentUserDeptId.equals(ticketDeptId)) {
            logger.debug("User {} is in same department as ticket {}", currentUserId, ticket.getId());
            return true;
        }

        return false;
    }

    public boolean canModifyTicket(Ticket ticket) {
        if (ticket == null) {
            return false;
        }
        
        if (isCurrentUserAdmin()) {
            return true;
        }
        
        User currentUser = getCurrentUser();
        return canModifyTicket(currentUser, ticket);
    }

    public boolean canModifyTicket(User currentUser, Ticket ticket) {
        if (!canViewTicket(currentUser, ticket)) {
            return false;
        }

        Long currentUserId = currentUser.getId();
        Long ticketCreatorId = ticket.getCreator() != null ? ticket.getCreator().getId() : null;
        Long ticketAssigneeId = ticket.getAssignee() != null ? ticket.getAssignee().getId() : null;

        if (currentUserId.equals(ticketCreatorId)) {
            return true;
        }

        if (currentUserId.equals(ticketAssigneeId)) {
            return true;
        }

        Department currentUserDept = currentUser.getDepartment();
        Department ticketDept = ticket.getDepartment();
        if (currentUserDept != null && ticketDept != null && 
            currentUserDept.getId().equals(ticketDept.getId())) {
            Integer userLevel = currentUser.getLevel();
            return userLevel != null && userLevel >= 2;
        }

        return false;
    }

    public boolean canDeleteTicket(Ticket ticket) {
        if (ticket == null) {
            return false;
        }
        
        if (isCurrentUserAdmin()) {
            return true;
        }
        
        User currentUser = getCurrentUser();
        Long ticketCreatorId = ticket.getCreator() != null ? ticket.getCreator().getId() : null;
        
        return currentUser.getId().equals(ticketCreatorId);
    }

    public boolean canAssignTicket(Ticket ticket) {
        if (ticket == null) {
            return false;
        }
        
        if (isCurrentUserAdmin()) {
            return true;
        }
        
        User currentUser = getCurrentUser();
        Department currentUserDept = currentUser.getDepartment();
        Department ticketDept = ticket.getDepartment();
        
        if (currentUserDept != null && ticketDept != null && 
            currentUserDept.getId().equals(ticketDept.getId())) {
            Integer userLevel = currentUser.getLevel();
            return userLevel != null && userLevel >= 2;
        }
        
        return false;
    }

    public Ticket getTicketWithPermissionCheck(Long ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
            .orElseThrow(() -> new IllegalArgumentException("工单不存在: " + ticketId));
        
        if (!canViewTicket(ticket)) {
            logger.warn("Permission denied for user to view ticket: {}", ticketId);
            throw new SecurityException("权限不足：您无权访问该工单");
        }
        
        return ticket;
    }

    public Ticket getTicketForModifyWithPermissionCheck(Long ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
            .orElseThrow(() -> new IllegalArgumentException("工单不存在: " + ticketId));
        
        if (!canModifyTicket(ticket)) {
            logger.warn("Permission denied for user to modify ticket: {}", ticketId);
            throw new SecurityException("权限不足：您无权修改该工单");
        }
        
        return ticket;
    }

    public List<Ticket> filterTicketsByPermission(List<Ticket> tickets) {
        if (tickets == null || tickets.isEmpty()) {
            return Collections.emptyList();
        }
        
        if (isCurrentUserAdmin()) {
            return tickets;
        }
        
        return tickets.stream()
            .filter(this::canViewTicket)
            .collect(Collectors.toList());
    }

    public void validateBatchOperation(List<Long> ticketIds, String operation) {
        if (ticketIds == null || ticketIds.isEmpty()) {
            return;
        }
        
        if (isCurrentUserAdmin()) {
            return;
        }
        
        for (Long ticketId : ticketIds) {
            Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("工单不存在: " + ticketId));
            
            if (!canModifyTicket(ticket)) {
                logger.warn("Permission denied for user to {} ticket: {} in batch operation", 
                    operation, ticketId);
                throw new SecurityException(
                    String.format("权限不足：您无权对工单 [%s] 执行 %s 操作", 
                        ticket.getTicketNo(), operation)
                );
            }
        }
    }

    public DataPermissionContext getCurrentUserPermissionContext() {
        User currentUser = getCurrentUser();
        return new DataPermissionContext(
            currentUser.getId(),
            currentUser.getDepartment() != null ? currentUser.getDepartment().getId() : null,
            currentUser.getLevel(),
            isCurrentUserAdmin()
        );
    }

    public static class DataPermissionContext {
        private final Long userId;
        private final Long departmentId;
        private final Integer userLevel;
        private final boolean admin;

        public DataPermissionContext(Long userId, Long departmentId, Integer userLevel, boolean admin) {
            this.userId = userId;
            this.departmentId = departmentId;
            this.userLevel = userLevel;
            this.admin = admin;
        }

        public Long getUserId() { return userId; }
        public Long getDepartmentId() { return departmentId; }
        public Integer getUserLevel() { return userLevel; }
        public boolean isAdmin() { return admin; }
    }
}
