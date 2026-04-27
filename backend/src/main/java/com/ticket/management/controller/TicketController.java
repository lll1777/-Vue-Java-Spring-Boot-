package com.ticket.management.controller;

import com.ticket.management.entity.Ticket;
import com.ticket.management.entity.TicketHistory;
import com.ticket.management.security.UserPrincipal;
import com.ticket.management.service.TicketService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tickets")
@CrossOrigin(origins = "*")
public class TicketController {

    private static final Logger logger = LoggerFactory.getLogger(TicketController.class);

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('TICKET_CREATE', 'ROLE_ADMIN')")
    public ResponseEntity<?> createTicket(
            @Valid @RequestBody TicketService.TicketCreateRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        Ticket ticket = ticketService.createTicket(request, userPrincipal.getId());
        
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
        Ticket ticket = ticketService.getTicket(id);
        return ResponseEntity.ok(ticket);
    }

    @GetMapping("/no/{ticketNo}")
    @PreAuthorize("hasAnyAuthority('TICKET_VIEW', 'ROLE_ADMIN')")
    public ResponseEntity<?> getTicketByNo(@PathVariable String ticketNo) {
        Ticket ticket = ticketService.getTicketByNo(ticketNo);
        return ResponseEntity.ok(ticket);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('TICKET_EDIT', 'ROLE_ADMIN')")
    public ResponseEntity<?> updateTicket(
            @PathVariable Long id,
            @Valid @RequestBody TicketService.TicketUpdateRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        Ticket ticket = ticketService.updateTicket(id, request, userPrincipal.getId());
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "工单更新成功");
        response.put("ticket", ticket);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/assign")
    @PreAuthorize("hasAnyAuthority('TICKET_ASSIGN', 'ROLE_ADMIN')")
    public ResponseEntity<?> assignTicket(
            @PathVariable Long id,
            @RequestBody AssignRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        Ticket ticket = ticketService.assignTicket(
            id, 
            request.getAssigneeId(), 
            userPrincipal.getId(), 
            request.getComments()
        );
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "工单分配成功");
        response.put("ticket", ticket);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/status")
    @PreAuthorize("hasAnyAuthority('TICKET_EDIT_STATUS', 'ROLE_ADMIN')")
    public ResponseEntity<?> updateStatus(
            @PathVariable Long id,
            @RequestBody StatusUpdateRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        Ticket ticket = ticketService.updateStatus(
            id, 
            request.getAction(), 
            userPrincipal.getId(), 
            request.getComments()
        );
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "工单状态更新成功");
        response.put("ticket", ticket);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getMyTickets(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        
        Sort sort = sortDir.equalsIgnoreCase("asc") 
            ? Sort.by(sortBy).ascending() 
            : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<Ticket> tickets = ticketService.getTicketsByAssignee(userPrincipal.getId(), pageable);
        
        return ResponseEntity.ok(tickets);
    }

    @GetMapping("/created")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getCreatedTickets(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        
        Sort sort = sortDir.equalsIgnoreCase("asc") 
            ? Sort.by(sortBy).ascending() 
            : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<Ticket> tickets = ticketService.getTicketsByCreator(userPrincipal.getId(), pageable);
        
        return ResponseEntity.ok(tickets);
    }

    @GetMapping("/{id}/history")
    @PreAuthorize("hasAnyAuthority('TICKET_VIEW', 'ROLE_ADMIN')")
    public ResponseEntity<?> getTicketHistory(@PathVariable Long id) {
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
