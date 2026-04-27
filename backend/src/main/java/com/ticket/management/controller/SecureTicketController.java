package com.ticket.management.controller;

import com.ticket.management.entity.Ticket;
import com.ticket.management.entity.TicketHistory;
import com.ticket.management.entity.User;
import com.ticket.management.repository.UserRepository;
import com.ticket.management.security.DataPermissionService;
import com.ticket.management.security.TicketQuerySpecification;
import com.ticket.management.service.TicketService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.*;

@RestController
@RequestMapping("/api/tickets")
@CrossOrigin(origins = "*")
public class SecureTicketController {

    private static final Logger logger = LoggerFactory.getLogger(SecureTicketController.class);

    private final TicketService ticketService;
    private final UserRepository userRepository;
    private final DataPermissionService dataPermissionService;
    private final TicketQuerySpecification ticketQuerySpecification;

    public SecureTicketController(
            TicketService ticketService,
            UserRepository userRepository,
            DataPermissionService dataPermissionService,
            TicketQuerySpecification ticketQuerySpecification) {
        this.ticketService = ticketService;
        this.userRepository = userRepository;
        this.dataPermissionService = dataPermissionService;
        this.ticketQuerySpecification = ticketQuerySpecification;
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new SecurityException("用户未认证");
        }
        
        String username = authentication.getName();
        return userRepository.findByUsername(username)
            .orElseThrow(() -> new SecurityException("用户不存在: " + username));
    }

    private ResponseEntity<Map<String, Object>> buildForbiddenResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("code", HttpStatus.FORBIDDEN.value());
        response.put("message", message);
        response.put("timestamp", new Date());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('TICKET_CREATE', 'ROLE_ADMIN')")
    public ResponseEntity<?> createTicket(
            @Valid @RequestBody TicketService.TicketCreateRequest request,
            Authentication authentication) {
        
        User currentUser = getCurrentUser();
        
        logger.info("User {} creating ticket in department: {}", 
            currentUser.getUsername(), request.getDepartmentId());
        
        if (!dataPermissionService.isCurrentUserAdmin()) {
            Long userDeptId = currentUser.getDepartment() != null ? 
                currentUser.getDepartment().getId() : null;
            
            if (userDeptId == null) {
                return buildForbiddenResponse("您没有所属部门，无法创建工单");
            }
            
            if (!userDeptId.equals(request.getDepartmentId())) {
                logger.warn("User {} attempted to create ticket in different department. User dept: {}, Request dept: {}",
                    currentUser.getUsername(), userDeptId, request.getDepartmentId());
                return buildForbiddenResponse("权限不足：只能在所属部门创建工单");
            }
        }
        
        Ticket ticket = ticketService.createTicket(request, currentUser.getId());
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "工单创建成功");
        response.put("ticketId", ticket.getId());
        response.put("ticketNo", ticket.getTicketNo());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('TICKET_VIEW', 'ROLE_ADMIN')")
    public ResponseEntity<?> getTicket(@PathVariable Long id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        logger.info("User {} requesting ticket: {}", authentication.getName(), id);
        
        Ticket ticket = dataPermissionService.getTicketWithPermissionCheck(id);
        
        return ResponseEntity.ok(ticket);
    }

    @GetMapping("/no/{ticketNo}")
    @PreAuthorize("hasAnyAuthority('TICKET_VIEW', 'ROLE_ADMIN')")
    public ResponseEntity<?> getTicketByNo(@PathVariable String ticketNo) {
        Ticket ticket = ticketService.getTicketByNo(ticketNo);
        
        if (!dataPermissionService.canViewTicket(ticket)) {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            logger.warn("User {} attempted to view unauthorized ticket: {}", 
                authentication.getName(), ticketNo);
            return buildForbiddenResponse("权限不足：您无权查看该工单");
        }
        
        return ResponseEntity.ok(ticket);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('TICKET_EDIT', 'ROLE_ADMIN')")
    public ResponseEntity<?> updateTicket(
            @PathVariable Long id,
            @Valid @RequestBody TicketService.TicketUpdateRequest request,
            Authentication authentication) {
        
        logger.info("User {} updating ticket: {}", authentication.getName(), id);
        
        Ticket ticket = dataPermissionService.getTicketForModifyWithPermissionCheck(id);
        
        User currentUser = getCurrentUser();
        
        Ticket updatedTicket = ticketService.updateTicket(id, request, currentUser.getId());
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "工单更新成功");
        response.put("ticket", updatedTicket);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/assign")
    @PreAuthorize("hasAnyAuthority('TICKET_ASSIGN', 'ROLE_ADMIN')")
    public ResponseEntity<?> assignTicket(
            @PathVariable Long id,
            @RequestBody AssignRequest request,
            Authentication authentication) {
        
        logger.info("User {} assigning ticket: {} to {}", 
            authentication.getName(), id, request.getAssigneeId());
        
        Ticket ticket = dataPermissionService.getTicketForModifyWithPermissionCheck(id);
        
        if (!dataPermissionService.canAssignTicket(ticket)) {
            logger.warn("User {} attempted to assign ticket without permission: {}", 
                authentication.getName(), id);
            return buildForbiddenResponse("权限不足：您无权分配该工单");
        }
        
        User currentUser = getCurrentUser();
        
        Ticket assignedTicket = ticketService.assignTicket(
            id, 
            request.getAssigneeId(), 
            currentUser.getId(), 
            request.getComments()
        );
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "工单分配成功");
        response.put("ticket", assignedTicket);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/status")
    @PreAuthorize("hasAnyAuthority('TICKET_EDIT_STATUS', 'ROLE_ADMIN')")
    public ResponseEntity<?> updateStatus(
            @PathVariable Long id,
            @RequestBody StatusUpdateRequest request,
            Authentication authentication) {
        
        logger.info("User {} updating ticket status: {} - action: {}", 
            authentication.getName(), id, request.getAction());
        
        Ticket ticket = dataPermissionService.getTicketForModifyWithPermissionCheck(id);
        
        User currentUser = getCurrentUser();
        
        Ticket updatedTicket = ticketService.updateStatus(
            id, 
            request.getAction(), 
            currentUser.getId(), 
            request.getComments()
        );
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "工单状态更新成功");
        response.put("ticket", updatedTicket);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getMyTickets(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        
        User currentUser = getCurrentUser();
        
        Sort sort = sortDir.equalsIgnoreCase("asc") 
            ? Sort.by(sortBy).ascending() 
            : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<Ticket> tickets = ticketService.getTicketsByAssignee(currentUser.getId(), pageable);
        
        return ResponseEntity.ok(tickets);
    }

    @GetMapping("/created")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getCreatedTickets(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        
        User currentUser = getCurrentUser();
        
        Sort sort = sortDir.equalsIgnoreCase("asc") 
            ? Sort.by(sortBy).ascending() 
            : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<Ticket> tickets = ticketService.getTicketsByCreator(currentUser.getId(), pageable);
        
        return ResponseEntity.ok(tickets);
    }

    @GetMapping("/department")
    @PreAuthorize("hasAnyAuthority('TICKET_VIEW', 'ROLE_ADMIN')")
    public ResponseEntity<?> getDepartmentTickets(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        
        User currentUser = getCurrentUser();
        Long departmentId = currentUser.getDepartment() != null ? 
            currentUser.getDepartment().getId() : null;
        
        if (departmentId == null && !dataPermissionService.isCurrentUserAdmin()) {
            return buildForbiddenResponse("您没有所属部门");
        }
        
        Sort sort = sortDir.equalsIgnoreCase("asc") 
            ? Sort.by(sortBy).ascending() 
            : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        if (dataPermissionService.isCurrentUserAdmin()) {
            return ResponseEntity.ok(ticketService.getTicketsByCreator(null, pageable));
        }
        
        Page<Ticket> tickets = ticketService.getTicketsByDepartment(departmentId, pageable);
        
        return ResponseEntity.ok(tickets);
    }

    @GetMapping("/{id}/history")
    @PreAuthorize("hasAnyAuthority('TICKET_VIEW', 'ROLE_ADMIN')")
    public ResponseEntity<?> getTicketHistory(@PathVariable Long id) {
        Ticket ticket = dataPermissionService.getTicketWithPermissionCheck(id);
        
        List<TicketHistory> history = ticketService.getTicketHistory(id);
        return ResponseEntity.ok(history);
    }

    public static class AssignRequest {
        private Long assigneeId;
        private String comments;

        public Long getAssigneeId() { return assigneeId; }
        public void setAssigneeId(Long assigneeId) { this.assigneeId = assigneeId; }
        public String getComments() { return comments; }
        public void setComments(String comments) { this.comments = comments; }
    }

    public static class StatusUpdateRequest {
        private String action;
        private String comments;

        public String getAction() { return action; }
        public void setAction(String action) { this.action = action; }
        public String getComments() { return comments; }
        public void setComments(String comments) { this.comments = comments; }
    }
}
