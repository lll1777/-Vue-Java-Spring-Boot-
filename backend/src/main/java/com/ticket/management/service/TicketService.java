package com.ticket.management.service;

import com.ticket.management.concurrency.DistributedLockService;
import com.ticket.management.entity.*;
import com.ticket.management.enums.TicketPriority;
import com.ticket.management.enums.TicketStatus;
import com.ticket.management.repository.*;
import com.ticket.management.statemachine.TicketStateMachine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class TicketService {

    private static final Logger logger = LoggerFactory.getLogger(TicketService.class);

    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final TicketCategoryRepository ticketCategoryRepository;
    private final TicketHistoryRepository ticketHistoryRepository;
    private final SLARuleRepository slaRuleRepository;
    private final TicketStateMachine ticketStateMachine;
    private final DistributedLockService distributedLockService;

    public TicketService(
            TicketRepository ticketRepository,
            UserRepository userRepository,
            DepartmentRepository departmentRepository,
            TicketCategoryRepository ticketCategoryRepository,
            TicketHistoryRepository ticketHistoryRepository,
            SLARuleRepository slaRuleRepository,
            TicketStateMachine ticketStateMachine,
            DistributedLockService distributedLockService) {
        this.ticketRepository = ticketRepository;
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.ticketCategoryRepository = ticketCategoryRepository;
        this.ticketHistoryRepository = ticketHistoryRepository;
        this.slaRuleRepository = slaRuleRepository;
        this.ticketStateMachine = ticketStateMachine;
        this.distributedLockService = distributedLockService;
    }

    @Transactional
    @Retryable(
        retryFor = ObjectOptimisticLockingFailureException.class,
        maxAttempts = 3,
        backoff = @Backoff(delay = 100, multiplier = 2)
    )
    public Ticket createTicket(TicketCreateRequest request, Long creatorId) {
        User creator = userRepository.findById(creatorId)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + creatorId));

        Department department = departmentRepository.findById(request.getDepartmentId())
            .orElseThrow(() -> new IllegalArgumentException("Department not found: " + request.getDepartmentId()));

        TicketCategory category = null;
        if (request.getCategoryId() != null) {
            category = ticketCategoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("Category not found: " + request.getCategoryId()));
        }

        Ticket ticket = new Ticket();
        ticket.setTicketNo(generateTicketNo());
        ticket.setTitle(request.getTitle());
        ticket.setDescription(request.getDescription());
        ticket.setStatus(TicketStatus.DRAFT);
        ticket.setPriority(request.getPriority() != null ? request.getPriority() : TicketPriority.MEDIUM);
        ticket.setCreator(creator);
        ticket.setDepartment(department);
        ticket.setCategory(category);
        ticket.setCustomerName(request.getCustomerName());
        ticket.setCustomerPhone(request.getCustomerPhone());
        ticket.setCustomerEmail(request.getCustomerEmail());
        ticket.setSource(request.getSource());
        ticket.setTags(request.getTags());
        ticket.setVersion(0L);

        applySLA(ticket, category);

        ticket = ticketRepository.save(ticket);

        createHistory(ticket, "CREATE", "创建工单", creator, null, null);

        logger.info("Created ticket: {} by user: {}", ticket.getTicketNo(), creatorId);
        
        return ticket;
    }

    @Transactional
    @Retryable(
        retryFor = ObjectOptimisticLockingFailureException.class,
        maxAttempts = 3,
        backoff = @Backoff(delay = 100, multiplier = 2)
    )
    public Ticket updateTicket(Long ticketId, TicketUpdateRequest request, Long operatorId) {
        return distributedLockService.executeWithTicketLock(ticketId, () -> {
            Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found: " + ticketId));

            User operator = userRepository.findById(operatorId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + operatorId));

            StringBuilder changes = new StringBuilder();

            if (request.getTitle() != null && !request.getTitle().equals(ticket.getTitle())) {
                changes.append("标题: ").append(ticket.getTitle()).append(" -> ").append(request.getTitle()).append("; ");
                ticket.setTitle(request.getTitle());
            }

            if (request.getDescription() != null && !request.getDescription().equals(ticket.getDescription())) {
                changes.append("描述已更新; ");
                ticket.setDescription(request.getDescription());
            }

            if (request.getPriority() != null && !request.getPriority().equals(ticket.getPriority())) {
                changes.append("优先级: ").append(ticket.getPriority()).append(" -> ").append(request.getPriority()).append("; ");
                ticket.setPriority(request.getPriority());
                applySLA(ticket, ticket.getCategory());
            }

            if (changes.length() > 0) {
                ticket = ticketRepository.save(ticket);
                createHistory(ticket, "UPDATE", changes.toString(), operator, null, null);
                logger.info("Updated ticket: {} by user: {}", ticketId, operatorId);
            }

            return ticket;
        });
    }

    @Transactional
    @Retryable(
        retryFor = ObjectOptimisticLockingFailureException.class,
        maxAttempts = 3,
        backoff = @Backoff(delay = 100, multiplier = 2)
    )
    public Ticket assignTicket(Long ticketId, Long assigneeId, Long operatorId, String comments) {
        return distributedLockService.executeWithTicketLock(ticketId, () -> {
            Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found: " + ticketId));

            User operator = userRepository.findById(operatorId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + operatorId));

            User assignee = userRepository.findById(assigneeId)
                .orElseThrow(() -> new IllegalArgumentException("Assignee not found: " + assigneeId));

            TicketStatus currentStatus = ticket.getStatus();
            TicketStatus newStatus = TicketStatus.ASSIGNED;

            if (!ticketStateMachine.canTransition(currentStatus, newStatus)) {
                throw new IllegalStateException(
                    String.format("Cannot transition ticket from %s to %s", currentStatus, newStatus)
                );
            }

            User oldAssignee = ticket.getAssignee();
            ticket.setAssignee(assignee);
            ticket.setStatus(newStatus);
            
            if (ticket.getResponseTime() == null) {
                ticket.setResponseTime(LocalDateTime.now());
            }

            ticket = ticketRepository.save(ticket);

            createHistory(ticket, "ASSIGN", comments != null ? comments : "分配工单", operator, oldAssignee, assignee);

            logger.info("Assigned ticket: {} to user: {} by operator: {}", ticketId, assigneeId, operatorId);
            
            return ticket;
        });
    }

    @Transactional
    @Retryable(
        retryFor = ObjectOptimisticLockingFailureException.class,
        maxAttempts = 3,
        backoff = @Backoff(delay = 100, multiplier = 2)
    )
    public Ticket updateStatus(Long ticketId, String action, Long operatorId, String comments) {
        return distributedLockService.executeWithTicketLock(ticketId, () -> {
            Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found: " + ticketId));

            User operator = userRepository.findById(operatorId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + operatorId));

            TicketStateMachine.TransitionContext context = new TicketStateMachine.TransitionContext(ticketId, operatorId);
            context.setComments(comments);

            TicketStateMachine.TransitionResult result = ticketStateMachine.executeAction(
                ticket.getStatus(), action, context
            );

            if (!result.isSuccess()) {
                throw new IllegalStateException("State transition failed: " + result.getMessage());
            }

            TicketStatus oldStatus = ticket.getStatus();
            ticket.setStatus(result.getToStatus());

            if (result.getToStatus() == TicketStatus.RESOLVED) {
                ticket.setActualResolveTime(LocalDateTime.now());
            }

            if (result.getToStatus() == TicketStatus.IN_PROGRESS && ticket.getResponseTime() == null) {
                ticket.setResponseTime(LocalDateTime.now());
            }

            ticket = ticketRepository.save(ticket);

            String actionDescription = getActionDescription(action, comments);
            createHistory(ticket, action.toUpperCase(), actionDescription, operator, null, null);

            logger.info("Updated ticket: {} status from {} to {} by user: {}", 
                ticketId, oldStatus, result.getToStatus(), operatorId);
            
            return ticket;
        });
    }

    public Ticket getTicket(Long ticketId) {
        return ticketRepository.findById(ticketId)
            .orElseThrow(() -> new IllegalArgumentException("Ticket not found: " + ticketId));
    }

    public Ticket getTicketByNo(String ticketNo) {
        return ticketRepository.findByTicketNo(ticketNo)
            .orElseThrow(() -> new IllegalArgumentException("Ticket not found: " + ticketNo));
    }

    public Page<Ticket> getTicketsByCreator(Long creatorId, Pageable pageable) {
        return ticketRepository.findByCreatorId(creatorId, pageable);
    }

    public Page<Ticket> getTicketsByAssignee(Long assigneeId, Pageable pageable) {
        return ticketRepository.findByAssigneeId(assigneeId, pageable);
    }

    public Page<Ticket> getTicketsByDepartment(Long departmentId, Pageable pageable) {
        return ticketRepository.findByDepartmentId(departmentId, pageable);
    }

    public List<TicketHistory> getTicketHistory(Long ticketId) {
        return ticketHistoryRepository.findByTicketIdOrderByActionTimeDesc(ticketId);
    }

    private void applySLA(Ticket ticket, TicketCategory category) {
        SLARule rule = findSLARule(ticket, category);
        
        if (rule != null) {
            LocalDateTime now = LocalDateTime.now();
            ticket.setSlaDeadline(now.plusMinutes(rule.getResolutionMinutes()));
            ticket.setSlaWarningSent(false);
            ticket.setSlaOverdue(false);
        }
    }

    private SLARule findSLARule(Ticket ticket, TicketCategory category) {
        Integer priorityLevel = ticket.getPriority() != null ? ticket.getPriority().getLevel() : null;
        Long categoryId = category != null ? category.getId() : null;

        List<SLARule> rules = slaRuleRepository.findMatchingRules(categoryId, priorityLevel);
        
        if (!rules.isEmpty()) {
            return rules.get(0);
        }

        return slaRuleRepository.findByIsDefaultTrueAndEnabledTrue()
            .orElse(null);
    }

    private void createHistory(
            Ticket ticket, 
            String action, 
            String description, 
            User operator, 
            User oldAssignee, 
            User newAssignee) {
        TicketHistory history = new TicketHistory();
        history.setTicket(ticket);
        history.setAction(action);
        history.setActionDescription(description);
        history.setOperator(operator);
        history.setOldAssignee(oldAssignee);
        history.setNewAssignee(newAssignee);
        history.setActionTime(LocalDateTime.now());
        ticketHistoryRepository.save(history);
    }

    private String generateTicketNo() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        String timestamp = LocalDateTime.now().format(formatter);
        String random = String.format("%06d", new Random().nextInt(1000000));
        return "TK" + timestamp + random;
    }

    private String getActionDescription(String action, String comments) {
        Map<String, String> descriptions = new HashMap<>();
        descriptions.put("submit", "提交工单");
        descriptions.put("approve", "审批通过");
        descriptions.put("reject", "审批拒绝");
        descriptions.put("assign", "分配工单");
        descriptions.put("start", "开始处理");
        descriptions.put("submit_review", "提交审核");
        descriptions.put("resolve", "解决工单");
        descriptions.put("close", "关闭工单");
        descriptions.put("cancel", "取消工单");
        descriptions.put("escalate", "升级工单");
        descriptions.put("reopen", "重新打开");
        descriptions.put("modify", "修改工单");

        String base = descriptions.getOrDefault(action.toLowerCase(), action);
        return comments != null ? base + ": " + comments : base;
    }

    public static class TicketCreateRequest {
        private String title;
        private String description;
        private TicketPriority priority;
        private Long departmentId;
        private Long categoryId;
        private String customerName;
        private String customerPhone;
        private String customerEmail;
        private String source;
        private String tags;

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public TicketPriority getPriority() { return priority; }
        public void setPriority(TicketPriority priority) { this.priority = priority; }
        public Long getDepartmentId() { return departmentId; }
        public void setDepartmentId(Long departmentId) { this.departmentId = departmentId; }
        public Long getCategoryId() { return categoryId; }
        public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
        public String getCustomerName() { return customerName; }
        public void setCustomerName(String customerName) { this.customerName = customerName; }
        public String getCustomerPhone() { return customerPhone; }
        public void setCustomerPhone(String customerPhone) { this.customerPhone = customerPhone; }
        public String getCustomerEmail() { return customerEmail; }
        public void setCustomerEmail(String customerEmail) { this.customerEmail = customerEmail; }
        public String getSource() { return source; }
        public void setSource(String source) { this.source = source; }
        public String getTags() { return tags; }
        public void setTags(String tags) { this.tags = tags; }
    }

    public static class TicketUpdateRequest {
        private String title;
        private String description;
        private TicketPriority priority;
        private String tags;

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public TicketPriority getPriority() { return priority; }
        public void setPriority(TicketPriority priority) { this.priority = priority; }
        public String getTags() { return tags; }
        public void setTags(String tags) { this.tags = tags; }
    }
}
