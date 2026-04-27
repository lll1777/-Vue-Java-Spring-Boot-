package com.ticket.management.service;

import com.ticket.management.concurrency.DistributedLockService;
import com.ticket.management.entity.*;
import com.ticket.management.enums.ApprovalStatus;
import com.ticket.management.enums.TicketStatus;
import com.ticket.management.repository.*;
import com.ticket.management.statemachine.TicketStateMachine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.Random;

@Service
public class ApprovalFlowService {

    private static final Logger logger = LoggerFactory.getLogger(ApprovalFlowService.class);

    private final ApprovalFlowRepository approvalFlowRepository;
    private final ApprovalInstanceRepository approvalInstanceRepository;
    private final ApprovalNodeRepository approvalNodeRepository;
    private final ApprovalRecordRepository approvalRecordRepository;
    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final TicketStateMachine ticketStateMachine;
    private final DistributedLockService distributedLockService;

    public ApprovalFlowService(
            ApprovalFlowRepository approvalFlowRepository,
            ApprovalInstanceRepository approvalInstanceRepository,
            ApprovalNodeRepository approvalNodeRepository,
            ApprovalRecordRepository approvalRecordRepository,
            TicketRepository ticketRepository,
            UserRepository userRepository,
            TicketStateMachine ticketStateMachine,
            DistributedLockService distributedLockService) {
        this.approvalFlowRepository = approvalFlowRepository;
        this.approvalInstanceRepository = approvalInstanceRepository;
        this.approvalNodeRepository = approvalNodeRepository;
        this.approvalRecordRepository = approvalRecordRepository;
        this.ticketRepository = ticketRepository;
        this.userRepository = userRepository;
        this.ticketStateMachine = ticketStateMachine;
        this.distributedLockService = distributedLockService;
    }

    @Transactional
    public ApprovalInstance startApproval(Long ticketId, String flowType, Long initiatorId) {
        return distributedLockService.executeWithTicketLock(ticketId, () -> {
            Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found: " + ticketId));
            
            User initiator = userRepository.findById(initiatorId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + initiatorId));

            Optional<ApprovalInstance> existingPending = approvalInstanceRepository
                .findByTicketIdAndStatus(ticketId, ApprovalStatus.PENDING);
            
            if (existingPending.isPresent()) {
                throw new IllegalStateException("Approval already in progress for ticket: " + ticketId);
            }

            ApprovalFlow flow = findMatchingFlow(ticket, flowType);

            if (flow.getNodes().isEmpty()) {
                autoApproveWithoutNodes(ticket, initiator);
                return null;
            }

            ApprovalInstance instance = createApprovalInstance(ticket, flow, initiator);
            
            advanceToNextNode(instance, 0);

            updateTicketStatus(ticket, TicketStatus.PENDING_APPROVAL, initiator);

            logger.info("Started approval instance {} for ticket {}", instance.getInstanceNo(), ticketId);
            
            return instance;
        });
    }

    @Transactional
    public ApprovalInstance approve(Long instanceId, Long approverId, String comments) {
        return distributedLockService.executeWithApprovalLock(instanceId, () -> {
            ApprovalInstance instance = approvalInstanceRepository.findByIdWithLock(instanceId)
                .orElseThrow(() -> new IllegalArgumentException("Approval instance not found: " + instanceId));

            if (instance.getStatus() != ApprovalStatus.PENDING) {
                throw new IllegalStateException("Approval is not in pending status: " + instance.getStatus());
            }

            ApprovalNode currentNode = instance.getCurrentNode();
            if (currentNode == null) {
                throw new IllegalStateException("No current node for approval instance");
            }

            User approver = userRepository.findById(approverId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + approverId));

            validateApprover(approver, currentNode, instance);

            createApprovalRecord(instance, currentNode, approver, ApprovalStatus.APPROVED, comments);

            List<ApprovalNode> nodes = instance.getFlow().getNodes();
            int currentIndex = instance.getCurrentNodeIndex();

            if (currentIndex >= nodes.size() - 1) {
                completeApproval(instance, ApprovalStatus.APPROVED);
                updateTicketStatus(instance.getTicket(), TicketStatus.APPROVED, approver);
            } else {
                advanceToNextNode(instance, currentIndex + 1);
            }

            logger.info("Approved instance {} by user {}", instanceId, approverId);
            
            return instance;
        });
    }

    @Transactional
    public ApprovalInstance reject(Long instanceId, Long approverId, String comments) {
        return distributedLockService.executeWithApprovalLock(instanceId, () -> {
            ApprovalInstance instance = approvalInstanceRepository.findByIdWithLock(instanceId)
                .orElseThrow(() -> new IllegalArgumentException("Approval instance not found: " + instanceId));

            if (instance.getStatus() != ApprovalStatus.PENDING) {
                throw new IllegalStateException("Approval is not in pending status: " + instance.getStatus());
            }

            ApprovalNode currentNode = instance.getCurrentNode();
            User approver = userRepository.findById(approverId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + approverId));

            validateApprover(approver, currentNode, instance);

            if (!currentNode.getCanReject()) {
                throw new IllegalStateException("Rejection is not allowed at this node");
            }

            createApprovalRecord(instance, currentNode, approver, ApprovalStatus.REJECTED, comments);

            completeApproval(instance, ApprovalStatus.REJECTED);
            
            updateTicketStatus(instance.getTicket(), TicketStatus.REJECTED, approver);

            logger.info("Rejected instance {} by user {}", instanceId, approverId);
            
            return instance;
        });
    }

    @Transactional
    public ApprovalInstance forward(Long instanceId, Long approverId, Long targetUserId, String reason) {
        return distributedLockService.executeWithApprovalLock(instanceId, () -> {
            ApprovalInstance instance = approvalInstanceRepository.findByIdWithLock(instanceId)
                .orElseThrow(() -> new IllegalArgumentException("Approval instance not found: " + instanceId));

            if (instance.getStatus() != ApprovalStatus.PENDING) {
                throw new IllegalStateException("Approval is not in pending status: " + instance.getStatus());
            }

            ApprovalNode currentNode = instance.getCurrentNode();
            if (!currentNode.getCanForward()) {
                throw new IllegalStateException("Forwarding is not allowed at this node");
            }

            User approver = userRepository.findById(approverId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + approverId));

            User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new IllegalArgumentException("Target user not found: " + targetUserId));

            validateApprover(approver, currentNode, instance);

            ApprovalRecord record = createApprovalRecord(instance, currentNode, approver, ApprovalStatus.FORWARDED, reason);
            record.setForwarded(true);
            record.setForwardedToUser(targetUser);
            record.setForwardReason(reason);
            approvalRecordRepository.save(record);

            instance.setCurrentNodeIndex(0);
            
            ApprovalNode dynamicNode = new ApprovalNode();
            dynamicNode.setNodeName("转发审批");
            dynamicNode.setNodeCode("FORWARDED_" + targetUserId);
            dynamicNode.setApproverType("USER");
            dynamicNode.setApproverId(targetUserId);
            dynamicNode.setOrderIndex(0);
            
            instance.setCurrentNode(dynamicNode);
            approvalInstanceRepository.save(instance);

            logger.info("Forwarded instance {} from {} to {}", instanceId, approverId, targetUserId);
            
            return instance;
        });
    }

    private ApprovalFlow findMatchingFlow(Ticket ticket, String flowType) {
        Long categoryId = ticket.getCategory() != null ? ticket.getCategory().getId() : null;
        Integer priority = ticket.getPriority() != null ? ticket.getPriority().getLevel() : null;
        Long departmentId = ticket.getDepartment() != null ? ticket.getDepartment().getId() : null;

        List<ApprovalFlow> matchingFlows = approvalFlowRepository.findMatchingFlows(
            flowType, categoryId, priority, departmentId
        );

        if (!matchingFlows.isEmpty()) {
            return matchingFlows.get(0);
        }

        return approvalFlowRepository.findDefaultFlow(flowType)
            .orElseThrow(() -> new IllegalArgumentException("No approval flow found for type: " + flowType));
    }

    private ApprovalInstance createApprovalInstance(Ticket ticket, ApprovalFlow flow, User initiator) {
        ApprovalInstance instance = new ApprovalInstance();
        instance.setInstanceNo(generateInstanceNo());
        instance.setFlow(flow);
        instance.setTicket(ticket);
        instance.setStatus(ApprovalStatus.PENDING);
        instance.setInitiator(initiator);
        instance.setStartedAt(LocalDateTime.now());
        instance.setCurrentNodeIndex(0);
        
        return approvalInstanceRepository.save(instance);
    }

    private void advanceToNextNode(ApprovalInstance instance, int nodeIndex) {
        List<ApprovalNode> nodes = instance.getFlow().getNodes();
        if (nodeIndex >= nodes.size()) {
            return;
        }

        ApprovalNode nextNode = nodes.get(nodeIndex);
        instance.setCurrentNode(nextNode);
        instance.setCurrentNodeIndex(nodeIndex);
        approvalInstanceRepository.save(instance);

        logger.debug("Advanced approval instance {} to node {}", instance.getInstanceNo(), nextNode.getNodeName());
    }

    private void createApprovalRecord(
            ApprovalInstance instance, 
            ApprovalNode node, 
            User approver, 
            ApprovalStatus status, 
            String comments) {
        ApprovalRecord record = new ApprovalRecord();
        record.setInstance(instance);
        record.setNode(node);
        record.setApprover(approver);
        record.setStatus(status);
        record.setComments(comments);
        record.setActionAt(LocalDateTime.now());
        record.setOrderIndex(instance.getRecords().size() + 1);
        approvalRecordRepository.save(record);
    }

    private void completeApproval(ApprovalInstance instance, ApprovalStatus finalStatus) {
        instance.setStatus(finalStatus);
        instance.setCompletedAt(LocalDateTime.now());
        instance.setCurrentNode(null);
        approvalInstanceRepository.save(instance);
    }

    private void autoApproveWithoutNodes(Ticket ticket, User initiator) {
        updateTicketStatus(ticket, TicketStatus.APPROVED, initiator);
        logger.info("Auto-approved ticket {} (no approval nodes)", ticket.getId());
    }

    private void updateTicketStatus(Ticket ticket, TicketStatus newStatus, User operator) {
        if (!ticketStateMachine.canTransition(ticket.getStatus(), newStatus)) {
            throw new IllegalStateException(
                String.format("Cannot transition ticket from %s to %s", ticket.getStatus(), newStatus)
            );
        }
        
        ticket.setStatus(newStatus);
        ticketRepository.save(ticket);
    }

    private void validateApprover(User approver, ApprovalNode node, ApprovalInstance instance) {
        String approverType = node.getApproverType();
        
        switch (approverType) {
            case "USER":
                if (!approver.getId().equals(node.getApproverId())) {
                    throw new SecurityException("You are not authorized to approve at this node");
                }
                break;
                
            case "ROLE":
                boolean hasRole = approver.getRoles().stream()
                    .anyMatch(role -> role.getId().equals(node.getApproverRoleId()));
                if (!hasRole) {
                    throw new SecurityException("You don't have the required role to approve at this node");
                }
                break;
                
            case "DEPARTMENT":
                if (approver.getDepartment() == null || 
                    !approver.getDepartment().getId().equals(node.getApproverDepartmentId())) {
                    throw new SecurityException("You are not in the required department to approve at this node");
                }
                break;
                
            case "LEVEL":
                Integer userLevel = approver.getLevel();
                if (userLevel == null || userLevel < node.getRequiredLevel()) {
                    throw new SecurityException("You don't have the required level to approve at this node");
                }
                break;
                
            default:
                throw new IllegalArgumentException("Unknown approver type: " + approverType);
        }
    }

    private String generateInstanceNo() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        String timestamp = LocalDateTime.now().format(formatter);
        String random = String.format("%04d", new Random().nextInt(10000));
        return "AP" + timestamp + random;
    }
}
