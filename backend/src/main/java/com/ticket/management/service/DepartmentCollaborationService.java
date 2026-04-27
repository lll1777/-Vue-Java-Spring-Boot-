package com.ticket.management.service;

import com.ticket.management.entity.*;
import com.ticket.management.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class DepartmentCollaborationService {

    private static final Logger logger = LoggerFactory.getLogger(DepartmentCollaborationService.class);

    private final DepartmentRepository departmentRepository;
    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final TicketHistoryRepository ticketHistoryRepository;

    public DepartmentCollaborationService(
            DepartmentRepository departmentRepository,
            TicketRepository ticketRepository,
            UserRepository userRepository,
            TicketHistoryRepository ticketHistoryRepository) {
        this.departmentRepository = departmentRepository;
        this.ticketRepository = ticketRepository;
        this.userRepository = userRepository;
        this.ticketHistoryRepository = ticketHistoryRepository;
    }

    @Transactional
    public CollaborationRequest createCollaborationRequest(
            Long ticketId, 
            Long sourceDepartmentId,
            Long targetDepartmentId,
            Long initiatorId,
            String reason,
            List<Long> requiredUserIds) {
        
        Ticket ticket = ticketRepository.findById(ticketId)
            .orElseThrow(() -> new IllegalArgumentException("Ticket not found: " + ticketId));

        Department sourceDept = departmentRepository.findById(sourceDepartmentId)
            .orElseThrow(() -> new IllegalArgumentException("Source department not found: " + sourceDepartmentId));

        Department targetDept = departmentRepository.findById(targetDepartmentId)
            .orElseThrow(() -> new IllegalArgumentException("Target department not found: " + targetDepartmentId));

        User initiator = userRepository.findById(initiatorId)
            .orElseThrow(() -> new IllegalArgumentException("Initiator not found: " + initiatorId));

        if (!canCollaborate(sourceDept, targetDept)) {
            throw new IllegalStateException(
                String.format("Department %s cannot collaborate with %s", 
                    sourceDept.getName(), targetDept.getName())
            );
        }

        CollaborationRequest request = new CollaborationRequest();
        request.setRequestNo(generateRequestNo());
        request.setTicket(ticket);
        request.setSourceDepartment(sourceDept);
        request.setTargetDepartment(targetDept);
        request.setInitiator(initiator);
        request.setReason(reason);
        request.setStatus(CollaborationRequest.RequestStatus.PENDING);
        request.setCreatedAt(LocalDateTime.now());

        if (requiredUserIds != null && !requiredUserIds.isEmpty()) {
            List<User> requiredUsers = userRepository.findAllById(requiredUserIds);
            request.setRequiredUsers(new HashSet<>(requiredUsers));
        }

        createHistory(ticket, "发起协作", 
            String.format("向部门 [%s] 发起协作请求: %s", targetDept.getName(), reason),
            initiator);

        logger.info("Created collaboration request {} for ticket {} from {} to {}", 
            request.getRequestNo(), ticketId, sourceDept.getName(), targetDept.getName());

        return request;
    }

    @Transactional
    public CollaborationRequest approveCollaboration(
            Long requestId, 
            Long approverId, 
            String comments,
            Long assignedUserId) {
        
        CollaborationRequest request = getRequest(requestId);
        
        if (request.getStatus() != CollaborationRequest.RequestStatus.PENDING) {
            throw new IllegalStateException("Collaboration request is not in pending status: " + request.getStatus());
        }

        User approver = userRepository.findById(approverId)
            .orElseThrow(() -> new IllegalArgumentException("Approver not found: " + approverId));

        if (!isAuthorizedToApprove(approver, request)) {
            throw new SecurityException("You are not authorized to approve this collaboration request");
        }

        request.setStatus(CollaborationRequest.RequestStatus.ACCEPTED);
        request.setApprover(approver);
        request.setApprovedAt(LocalDateTime.now());
        request.setComments(comments);

        if (assignedUserId != null) {
            User assignedUser = userRepository.findById(assignedUserId)
                .orElseThrow(() -> new IllegalArgumentException("Assigned user not found: " + assignedUserId));
            request.setAssignedUser(assignedUser);

            Ticket ticket = request.getTicket();
            ticket.setAssignee(assignedUser);
            ticketRepository.save(ticket);
        }

        createHistory(request.getTicket(), "接受协作", 
            String.format("部门 [%s] 接受协作请求，处理人: %s", 
                request.getTargetDepartment().getName(), 
                request.getAssignedUser() != null ? request.getAssignedUser().getRealName() : "待定"),
            approver);

        logger.info("Approved collaboration request {} by user {}", requestId, approverId);

        return request;
    }

    @Transactional
    public CollaborationRequest rejectCollaboration(
            Long requestId, 
            Long approverId, 
            String reason) {
        
        CollaborationRequest request = getRequest(requestId);
        
        if (request.getStatus() != CollaborationRequest.RequestStatus.PENDING) {
            throw new IllegalStateException("Collaboration request is not in pending status: " + request.getStatus());
        }

        User approver = userRepository.findById(approverId)
            .orElseThrow(() -> new IllegalArgumentException("Approver not found: " + approverId));

        if (!isAuthorizedToApprove(approver, request)) {
            throw new SecurityException("You are not authorized to reject this collaboration request");
        }

        request.setStatus(CollaborationRequest.RequestStatus.REJECTED);
        request.setApprover(approver);
        request.setApprovedAt(LocalDateTime.now());
        request.setRejectReason(reason);

        createHistory(request.getTicket(), "拒绝协作", 
            String.format("部门 [%s] 拒绝协作请求，原因: %s", 
                request.getTargetDepartment().getName(), reason),
            approver);

        logger.info("Rejected collaboration request {} by user {}", requestId, approverId);

        return request;
    }

    @Transactional
    public CollaborationRequest completeCollaboration(Long requestId, Long operatorId, String result) {
        CollaborationRequest request = getRequest(requestId);
        
        if (request.getStatus() != CollaborationRequest.RequestStatus.ACCEPTED) {
            throw new IllegalStateException("Collaboration request is not in accepted status: " + request.getStatus());
        }

        User operator = userRepository.findById(operatorId)
            .orElseThrow(() -> new IllegalArgumentException("Operator not found: " + operatorId));

        request.setStatus(CollaborationRequest.RequestStatus.COMPLETED);
        request.setCompletedAt(LocalDateTime.now());
        request.setResult(result);

        createHistory(request.getTicket(), "完成协作", 
            String.format("部门 [%s] 完成协作处理，结果: %s", 
                request.getTargetDepartment().getName(), result),
            operator);

        logger.info("Completed collaboration request {} by user {}", requestId, operatorId);

        return request;
    }

    public List<CollaborationRequest> getOutgoingRequests(Long departmentId) {
        return null;
    }

    public List<CollaborationRequest> getIncomingRequests(Long departmentId) {
        return null;
    }

    public List<CollaborationRequest> getRequestsByTicket(Long ticketId) {
        return null;
    }

    @Transactional
    public void addCollaborator(Long departmentId, Long collaboratorId) {
        Department department = departmentRepository.findById(departmentId)
            .orElseThrow(() -> new IllegalArgumentException("Department not found: " + departmentId));

        Department collaborator = departmentRepository.findById(collaboratorId)
            .orElseThrow(() -> new IllegalArgumentException("Collaborator not found: " + collaboratorId));

        department.getCollaborators().add(collaborator);
        collaborator.getCollaborators().add(department);

        departmentRepository.save(department);
        departmentRepository.save(collaborator);

        logger.info("Added collaborator {} to department {}", collaboratorId, departmentId);
    }

    @Transactional
    public void removeCollaborator(Long departmentId, Long collaboratorId) {
        Department department = departmentRepository.findById(departmentId)
            .orElseThrow(() -> new IllegalArgumentException("Department not found: " + departmentId));

        Department collaborator = departmentRepository.findById(collaboratorId)
            .orElseThrow(() -> new IllegalArgumentException("Collaborator not found: " + collaboratorId));

        department.getCollaborators().remove(collaborator);
        collaborator.getCollaborators().remove(department);

        departmentRepository.save(department);
        departmentRepository.save(collaborator);

        logger.info("Removed collaborator {} from department {}", collaboratorId, departmentId);
    }

    public Set<Department> getCollaborators(Long departmentId) {
        Department department = departmentRepository.findById(departmentId)
            .orElseThrow(() -> new IllegalArgumentException("Department not found: " + departmentId));
        
        return department.getCollaborators();
    }

    public boolean canCollaborate(Department source, Department target) {
        if (source.getId().equals(target.getId())) {
            return true;
        }
        
        return source.getCollaborators().stream()
            .anyMatch(dept -> dept.getId().equals(target.getId()));
    }

    private CollaborationRequest getRequest(Long requestId) {
        return null;
    }

    private boolean isAuthorizedToApprove(User approver, CollaborationRequest request) {
        if (approver.getDepartment() == null) {
            return false;
        }
        
        return approver.getDepartment().getId().equals(request.getTargetDepartment().getId());
    }

    private void createHistory(Ticket ticket, String action, String description, User operator) {
        TicketHistory history = new TicketHistory();
        history.setTicket(ticket);
        history.setAction(action);
        history.setActionDescription(description);
        history.setOperator(operator);
        history.setActionTime(LocalDateTime.now());
        ticketHistoryRepository.save(history);
    }

    private String generateRequestNo() {
        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        String timestamp = LocalDateTime.now().format(formatter);
        String random = String.format("%04d", new Random().nextInt(10000));
        return "CR" + timestamp + random;
    }

    @Entity
    @Table(name = "collaboration_requests")
    public static class CollaborationRequest extends BaseEntity {

        public enum RequestStatus {
            PENDING,
            ACCEPTED,
            REJECTED,
            COMPLETED,
            CANCELLED
        }

        @Column(name = "request_no", unique = true, nullable = false, length = 32)
        private String requestNo;

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "ticket_id", nullable = false)
        private Ticket ticket;

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "source_department_id", nullable = false)
        private Department sourceDepartment;

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "target_department_id", nullable = false)
        private Department targetDepartment;

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "initiator_id", nullable = false)
        private User initiator;

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "approver_id")
        private User approver;

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "assigned_user_id")
        private User assignedUser;

        @ManyToMany
        @JoinTable(
            name = "collaboration_required_users",
            joinColumns = @JoinColumn(name = "request_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
        )
        private Set<User> requiredUsers = new HashSet<>();

        @Enumerated(EnumType.STRING)
        @Column(name = "status", nullable = false, length = 20)
        private RequestStatus status;

        @Column(name = "reason", length = 500)
        private String reason;

        @Column(name = "reject_reason", length = 500)
        private String rejectReason;

        @Column(name = "comments", length = 500)
        private String comments;

        @Column(name = "result", columnDefinition = "TEXT")
        private String result;

        @Column(name = "created_at")
        private LocalDateTime createdAt;

        @Column(name = "approved_at")
        private LocalDateTime approvedAt;

        @Column(name = "completed_at")
        private LocalDateTime completedAt;

        public String getRequestNo() { return requestNo; }
        public void setRequestNo(String requestNo) { this.requestNo = requestNo; }
        public Ticket getTicket() { return ticket; }
        public void setTicket(Ticket ticket) { this.ticket = ticket; }
        public Department getSourceDepartment() { return sourceDepartment; }
        public void setSourceDepartment(Department sourceDepartment) { this.sourceDepartment = sourceDepartment; }
        public Department getTargetDepartment() { return targetDepartment; }
        public void setTargetDepartment(Department targetDepartment) { this.targetDepartment = targetDepartment; }
        public User getInitiator() { return initiator; }
        public void setInitiator(User initiator) { this.initiator = initiator; }
        public User getApprover() { return approver; }
        public void setApprover(User approver) { this.approver = approver; }
        public User getAssignedUser() { return assignedUser; }
        public void setAssignedUser(User assignedUser) { this.assignedUser = assignedUser; }
        public Set<User> getRequiredUsers() { return requiredUsers; }
        public void setRequiredUsers(Set<User> requiredUsers) { this.requiredUsers = requiredUsers; }
        public RequestStatus getStatus() { return status; }
        public void setStatus(RequestStatus status) { this.status = status; }
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
        public String getRejectReason() { return rejectReason; }
        public void setRejectReason(String rejectReason) { this.rejectReason = rejectReason; }
        public String getComments() { return comments; }
        public void setComments(String comments) { this.comments = comments; }
        public String getResult() { return result; }
        public void setResult(String result) { this.result = result; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
        public LocalDateTime getApprovedAt() { return approvedAt; }
        public void setApprovedAt(LocalDateTime approvedAt) { this.approvedAt = approvedAt; }
        public LocalDateTime getCompletedAt() { return completedAt; }
        public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    }
}
