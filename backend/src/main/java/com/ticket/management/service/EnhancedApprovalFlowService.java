package com.ticket.management.service;

import com.ticket.management.concurrency.DistributedLockService;
import com.ticket.management.entity.*;
import com.ticket.management.enums.ApprovalStatus;
import com.ticket.management.enums.TicketStatus;
import com.ticket.management.repository.*;
import com.ticket.management.statemachine.ApprovalStateMachine;
import com.ticket.management.statemachine.ApprovalStateMachine.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class EnhancedApprovalFlowService {

    private static final Logger logger = LoggerFactory.getLogger(EnhancedApprovalFlowService.class);

    private final ApprovalFlowRepository approvalFlowRepository;
    private final ApprovalInstanceRepository approvalInstanceRepository;
    private final ApprovalNodeRepository approvalNodeRepository;
    private final ApprovalRecordRepository approvalRecordRepository;
    private final ApprovalSignNodeRepository approvalSignNodeRepository;
    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final TicketHistoryRepository ticketHistoryRepository;
    private final ApprovalStateMachine approvalStateMachine;
    private final DistributedLockService distributedLockService;

    public EnhancedApprovalFlowService(
            ApprovalFlowRepository approvalFlowRepository,
            ApprovalInstanceRepository approvalInstanceRepository,
            ApprovalNodeRepository approvalNodeRepository,
            ApprovalRecordRepository approvalRecordRepository,
            ApprovalSignNodeRepository approvalSignNodeRepository,
            TicketRepository ticketRepository,
            UserRepository userRepository,
            TicketHistoryRepository ticketHistoryRepository,
            ApprovalStateMachine approvalStateMachine,
            DistributedLockService distributedLockService) {
        this.approvalFlowRepository = approvalFlowRepository;
        this.approvalInstanceRepository = approvalInstanceRepository;
        this.approvalNodeRepository = approvalNodeRepository;
        this.approvalRecordRepository = approvalRecordRepository;
        this.approvalSignNodeRepository = approvalSignNodeRepository;
        this.ticketRepository = ticketRepository;
        this.userRepository = userRepository;
        this.ticketHistoryRepository = ticketHistoryRepository;
        this.approvalStateMachine = approvalStateMachine;
        this.distributedLockService = distributedLockService;
    }

    @Transactional
    public ApprovalInstance startMultiLevelApproval(Long ticketId, Long initiatorId) {
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

            ApprovalFlow flow = findMatchingMultiLevelFlow(ticket);

            if (flow.getNodes().isEmpty()) {
                autoApproveWithoutNodes(ticket, initiator);
                return null;
            }

            ApprovalInstance instance = createApprovalInstance(ticket, flow, initiator);

            advanceToNextNode(instance, 0);

            updateTicketStatus(ticket, TicketStatus.PENDING_APPROVAL, initiator, "提交审批");

            logger.info("Started multi-level approval instance {} for ticket {}", instance.getInstanceNo(), ticketId);

            return instance;
        });
    }

    @Transactional
    public ApprovalInstance approve(Long instanceId, Long approverId, String comments) {
        return distributedLockService.executeWithApprovalLock(instanceId, () -> {
            ApprovalInstance instance = approvalInstanceRepository.findByIdWithLock(instanceId)
                .orElseThrow(() -> new IllegalArgumentException("Approval instance not found: " + instanceId));

            if (instance.getStatus() != ApprovalStatus.PENDING && instance.getStatus() != ApprovalStatus.FORWARDED) {
                throw new IllegalStateException("Approval is not in pending/forwarded status: " + instance.getStatus());
            }

            User approver = userRepository.findById(approverId)
                .orElseThrow(() -> new IllegalArgumentException("Approver not found: " + approverId));

            Optional<ApprovalSignNode> activeSignNode = approvalSignNodeRepository
                .findByInstanceIdAndActiveTrue(instanceId);

            if (activeSignNode.isPresent()) {
                ApprovalSignNode signNode = activeSignNode.get();
                if (!signNode.getSignUser().getId().equals(approverId)) {
                    throw new SecurityException("You are not the signer for this step");
                }
                return processSignApproval(instance, signNode, approver, comments);
            }

            ApprovalNode currentNode = instance.getCurrentNode();
            if (currentNode == null) {
                throw new IllegalStateException("No current node for approval instance");
            }

            validateApprover(approver, currentNode, instance, "APPROVE");

            createApprovalRecord(instance, currentNode, approver, ApprovalStatus.APPROVED, comments);

            List<ApprovalNode> nodes = instance.getFlow().getNodes();
            int currentIndex = instance.getCurrentNodeIndex();

            if (currentIndex >= nodes.size() - 1) {
                completeApproval(instance, ApprovalStatus.APPROVED);
                updateTicketStatus(instance.getTicket(), TicketStatus.APPROVED, approver, "审批通过");
                logger.info("Approval instance {} completed (approved)", instanceId);
            } else {
                advanceToNextNode(instance, currentIndex + 1);
                logger.info("Approval instance {} advanced to next node", instanceId);
            }

            return instance;
        });
    }

    @Transactional
    public ApprovalInstance reject(
            Long instanceId, 
            Long approverId, 
            String comments, 
            RejectTarget rejectTarget, 
            Long targetNodeId) {
        
        return distributedLockService.executeWithApprovalLock(instanceId, () -> {
            ApprovalInstance instance = approvalInstanceRepository.findByIdWithLock(instanceId)
                .orElseThrow(() -> new IllegalArgumentException("Approval instance not found: " + instanceId));

            if (instance.getStatus() != ApprovalStatus.PENDING) {
                throw new IllegalStateException("Approval is not in pending status: " + instance.getStatus());
            }

            ApprovalNode currentNode = instance.getCurrentNode();
            User approver = userRepository.findById(approverId)
                .orElseThrow(() -> new IllegalArgumentException("Approver not found: " + approverId));

            validateApprover(approver, currentNode, instance, "REJECT");

            if (!currentNode.getCanReject()) {
                throw new IllegalStateException("Rejection is not allowed at this node");
            }

            createApprovalRecord(instance, currentNode, approver, ApprovalStatus.REJECTED, 
                String.format("驳回原因: %s, 驳回目标: %s", comments, rejectTarget));

            int targetIndex = determineRejectTargetIndex(instance, rejectTarget, targetNodeId);

            if (targetIndex == -1) {
                completeApproval(instance, ApprovalStatus.REJECTED);
                updateTicketStatus(instance.getTicket(), TicketStatus.REJECTED, approver, 
                    String.format("审批被驳回: %s", comments));
                logger.info("Approval instance {} rejected and returned to draft", instanceId);
            } else {
                returnToNode(instance, targetIndex, approver, comments);
                logger.info("Approval instance {} returned to node index {}", instanceId, targetIndex);
            }

            return instance;
        });
    }

    @Transactional
    public ApprovalInstance recall(Long instanceId, Long initiatorId, String reason) {
        return distributedLockService.executeWithApprovalLock(instanceId, () -> {
            ApprovalInstance instance = approvalInstanceRepository.findByIdWithLock(instanceId)
                .orElseThrow(() -> new IllegalArgumentException("Approval instance not found: " + instanceId));

            if (instance.getStatus() != ApprovalStatus.PENDING) {
                throw new IllegalStateException("Cannot recall: approval is not in pending status");
            }

            if (!instance.getInitiator().getId().equals(initiatorId)) {
                throw new SecurityException("Only initiator can recall the approval");
            }

            ApprovalNode currentNode = instance.getCurrentNode();
            if (currentNode != null && !currentNode.getCanRecall()) {
                throw new IllegalStateException("Recall is not allowed at current node");
            }

            User initiator = userRepository.findById(initiatorId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + initiatorId));

            if (currentNode != null) {
                createApprovalRecord(instance, currentNode, initiator, ApprovalStatus.RECALLED, 
                    String.format("发起人撤回: %s", reason));
            }

            completeApproval(instance, ApprovalStatus.RECALLED);
            updateTicketStatus(instance.getTicket(), TicketStatus.DRAFT, initiator, 
                String.format("审批被撤回: %s", reason));

            logger.info("Approval instance {} recalled by initiator {}", instanceId, initiatorId);

            return instance;
        });
    }

    @Transactional
    public ApprovalInstance addSign(
            Long instanceId, 
            Long operatorId, 
            Long signUserId, 
            SignType signType, 
            String reason) {
        
        return distributedLockService.executeWithApprovalLock(instanceId, () -> {
            ApprovalInstance instance = approvalInstanceRepository.findByIdWithLock(instanceId)
                .orElseThrow(() -> new IllegalArgumentException("Approval instance not found: " + instanceId));

            if (instance.getStatus() != ApprovalStatus.PENDING) {
                throw new IllegalStateException("Cannot add sign: approval is not in pending status");
            }

            ApprovalNode currentNode = instance.getCurrentNode();
            User operator = userRepository.findById(operatorId)
                .orElseThrow(() -> new IllegalArgumentException("Operator not found: " + operatorId));

            User signUser = userRepository.findById(signUserId)
                .orElseThrow(() -> new IllegalArgumentException("Sign user not found: " + signUserId));

            validateApprover(operator, currentNode, instance, "ADD_SIGN");

            if (!currentNode.getCanForward()) {
                throw new IllegalStateException("Add sign is not allowed at this node");
            }

            ApprovalSignNode signNode = new ApprovalSignNode();
            signNode.setInstance(instance);
            signNode.setOriginalNode(currentNode);
            signNode.setSignUser(signUser);
            signNode.setSignType(signType.name());
            signNode.setReason(reason);
            signNode.setOperator(operator);
            signNode.setActive(true);
            signNode.setCreatedAt(LocalDateTime.now());
            approvalSignNodeRepository.save(signNode);

            createApprovalRecord(instance, currentNode, operator, ApprovalStatus.FORWARDED,
                String.format("加签: 类型=%s, 加签人=%s, 原因=%s", 
                    signType, signUser.getRealName(), reason));

            instance.setStatus(ApprovalStatus.FORWARDED);
            approvalInstanceRepository.save(instance);

            logger.info("Added sign to approval instance {}: type={}, user={}", 
                instanceId, signType, signUserId);

            return instance;
        });
    }

    @Transactional
    public ApprovalInstance forward(
            Long instanceId, 
            Long approverId, 
            Long targetUserId, 
            String reason) {
        
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
                .orElseThrow(() -> new IllegalArgumentException("Approver not found: " + approverId));

            User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new IllegalArgumentException("Target user not found: " + targetUserId));

            validateApprover(approver, currentNode, instance, "FORWARD");

            ApprovalSignNode signNode = new ApprovalSignNode();
            signNode.setInstance(instance);
            signNode.setOriginalNode(currentNode);
            signNode.setSignUser(targetUser);
            signNode.setSignType(SignType.AFTER_SIGN.name());
            signNode.setReason(reason);
            signNode.setOperator(approver);
            signNode.setActive(true);
            signNode.setCreatedAt(LocalDateTime.now());
            approvalSignNodeRepository.save(signNode);

            createApprovalRecord(instance, currentNode, approver, ApprovalStatus.FORWARDED, reason);

            instance.setStatus(ApprovalStatus.FORWARDED);
            approvalInstanceRepository.save(instance);

            logger.info("Forwarded instance {} from {} to {}", instanceId, approverId, targetUserId);

            return instance;
        });
    }

    private ApprovalInstance processSignApproval(
            ApprovalInstance instance, 
            ApprovalSignNode signNode, 
            User approver, 
            String comments) {
        
        signNode.setActive(false);
        signNode.setApprovedAt(LocalDateTime.now());
        signNode.setComments(comments);
        approvalSignNodeRepository.save(signNode);

        createApprovalRecord(instance, signNode.getOriginalNode(), approver, 
            ApprovalStatus.APPROVED, String.format("加签审批通过: %s", comments));

        if (SignType.AFTER_SIGN.name().equals(signNode.getSignType())) {
            instance.setStatus(ApprovalStatus.PENDING);
            approvalInstanceRepository.save(instance);
            return instance;
        } else if (SignType.BEFORE_SIGN.name().equals(signNode.getSignType())) {
            instance.setStatus(ApprovalStatus.PENDING);
            approvalInstanceRepository.save(instance);
            return instance;
        }

        return instance;
    }

    private int determineRejectTargetIndex(ApprovalInstance instance, RejectTarget rejectTarget, Long targetNodeId) {
        List<ApprovalNode> nodes = instance.getFlow().getNodes();
        
        switch (rejectTarget) {
            case INITIATOR:
                return -1;
            case FIRST_NODE:
                return 0;
            case PREVIOUS_NODE:
                return Math.max(0, instance.getCurrentNodeIndex() - 1);
            case SPECIFIC_NODE:
                if (targetNodeId != null) {
                    for (int i = 0; i < nodes.size(); i++) {
                        if (nodes.get(i).getId().equals(targetNodeId)) {
                            return i;
                        }
                    }
                }
                return -1;
            default:
                return -1;
        }
    }

    private void returnToNode(ApprovalInstance instance, int targetIndex, User operator, String comments) {
        List<ApprovalNode> nodes = instance.getFlow().getNodes();
        if (targetIndex >= 0 && targetIndex < nodes.size()) {
            ApprovalNode targetNode = nodes.get(targetIndex);
            instance.setCurrentNode(targetNode);
            instance.setCurrentNodeIndex(targetIndex);
            instance.setStatus(ApprovalStatus.PENDING);
            approvalInstanceRepository.save(instance);

            Ticket ticket = instance.getTicket();
            createHistory(ticket, "驳回", 
                String.format("审批驳回至节点 [%s]: %s", targetNode.getNodeName(), comments),
                operator);

            logger.info("Approval instance {} returned to node {}", instance.getId(), targetIndex);
        }
    }

    private ApprovalFlow findMatchingMultiLevelFlow(Ticket ticket) {
        Long categoryId = ticket.getCategory() != null ? ticket.getCategory().getId() : null;
        Integer priority = ticket.getPriority() != null ? ticket.getPriority().getLevel() : null;
        Long departmentId = ticket.getDepartment() != null ? ticket.getDepartment().getId() : null;

        List<ApprovalFlow> matchingFlows = approvalFlowRepository.findMatchingFlows(
            "MULTI_LEVEL", categoryId, priority, departmentId
        );

        if (!matchingFlows.isEmpty()) {
            return matchingFlows.get(0);
        }

        return approvalFlowRepository.findDefaultFlow("MULTI_LEVEL")
            .orElseGet(() -> createDefaultMultiLevelFlow());
    }

    private ApprovalFlow createDefaultMultiLevelFlow() {
        ApprovalFlow flow = new ApprovalFlow();
        flow.setName("默认多级审批流");
        flow.setCode("DEFAULT_MULTI_LEVEL");
        flow.setFlowType("MULTI_LEVEL");
        flow.setDefault(true);
        flow.setEnabled(true);
        flow.setVersion(1);
        return flow;
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
        instance.setVersion(0L);
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
        instance.setStatus(ApprovalStatus.PENDING);
        approvalInstanceRepository.save(instance);

        logger.debug("Advanced approval instance {} to node {}: {}", 
            instance.getInstanceNo(), nodeIndex, nextNode.getNodeName());
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
        updateTicketStatus(ticket, TicketStatus.APPROVED, initiator, "自动审批通过（无审批节点）");
        logger.info("Auto-approved ticket {} (no approval nodes)", ticket.getId());
    }

    private void updateTicketStatus(Ticket ticket, TicketStatus newStatus, User operator, String reason) {
        ticket.setStatus(newStatus);
        ticketRepository.save(ticket);
        createHistory(ticket, "状态变更", reason, operator);
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

    private void validateApprover(User approver, ApprovalNode node, ApprovalInstance instance, String action) {
        String approverType = node.getApproverType();

        switch (approverType) {
            case "USER":
                if (!approver.getId().equals(node.getApproverId())) {
                    throw new SecurityException("You are not authorized to " + action + " at this node");
                }
                break;

            case "ROLE":
                boolean hasRole = approver.getRoles().stream()
                    .anyMatch(role -> role.getId().equals(node.getApproverRoleId()));
                if (!hasRole) {
                    throw new SecurityException("You don't have the required role to " + action);
                }
                break;

            case "DEPARTMENT":
                if (approver.getDepartment() == null ||
                    !approver.getDepartment().getId().equals(node.getApproverDepartmentId())) {
                    throw new SecurityException("You are not in the required department to " + action);
                }
                break;

            case "LEVEL":
                Integer userLevel = approver.getLevel();
                if (userLevel == null || userLevel < node.getRequiredLevel()) {
                    throw new SecurityException("You don't have the required level to " + action);
                }
                break;

            case "DEPARTMENT_HEAD":
                if (approver.getDepartment() == null ||
                    !isDepartmentHead(approver, node.getApproverDepartmentId())) {
                    throw new SecurityException("You are not the department head to " + action);
                }
                break;

            case "INITIATOR_DEPARTMENT":
                User initiator = instance.getInitiator();
                if (initiator.getDepartment() == null ||
                    approver.getDepartment() == null ||
                    !approver.getDepartment().getId().equals(initiator.getDepartment().getId())) {
                    throw new SecurityException("You are not in the same department as initiator");
                }
                break;

            default:
                throw new IllegalArgumentException("Unknown approver type: " + approverType);
        }
    }

    private boolean isDepartmentHead(User user, Long departmentId) {
        Integer userLevel = user.getLevel();
        return userLevel != null && userLevel >= 3;
    }

    private String generateInstanceNo() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        String timestamp = LocalDateTime.now().format(formatter);
        String random = String.format("%04d", new Random().nextInt(10000));
        return "AP" + timestamp + random;
    }
}
