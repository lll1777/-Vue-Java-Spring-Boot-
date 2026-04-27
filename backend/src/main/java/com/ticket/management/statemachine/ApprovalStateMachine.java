package com.ticket.management.statemachine;

import com.ticket.management.enums.ApprovalStatus;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.*;

@Component
public class ApprovalStateMachine {

    private final Map<ApprovalStatus, Set<ApprovalStatus>> transitionMap = new EnumMap<>(ApprovalStatus.class);
    private final Map<String, ApprovalTransitionAction> actionMap = new HashMap<>();

    public enum RejectTarget {
        INITIATOR,
        PREVIOUS_NODE,
        SPECIFIC_NODE,
        FIRST_NODE
    }

    public enum SignType {
        BEFORE_SIGN,
        AFTER_SIGN,
        PARALLEL_SIGN
    }

    @PostConstruct
    public void init() {
        initTransitions();
        initActions();
    }

    private void initTransitions() {
        addTransition(ApprovalStatus.PENDING, ApprovalStatus.APPROVED);
        addTransition(ApprovalStatus.PENDING, ApprovalStatus.REJECTED);
        addTransition(ApprovalStatus.PENDING, ApprovalStatus.FORWARDED);
        addTransition(ApprovalStatus.PENDING, ApprovalStatus.RECALLED);

        addTransition(ApprovalStatus.FORWARDED, ApprovalStatus.APPROVED);
        addTransition(ApprovalStatus.FORWARDED, ApprovalStatus.REJECTED);
        addTransition(ApprovalStatus.FORWARDED, ApprovalStatus.FORWARDED);

        addTransition(ApprovalStatus.APPROVED, ApprovalStatus.RECALLED);
        addTransition(ApprovalStatus.REJECTED, ApprovalStatus.PENDING);
    }

    private void addTransition(ApprovalStatus from, ApprovalStatus to) {
        transitionMap.computeIfAbsent(from, k -> EnumSet.noneOf(ApprovalStatus.class)).add(to);
    }

    private void initActions() {
        actionMap.put("approve", context -> ApprovalStatus.APPROVED);
        actionMap.put("reject", context -> ApprovalStatus.REJECTED);
        actionMap.put("forward", context -> ApprovalStatus.FORWARDED);
        actionMap.put("recall", context -> ApprovalStatus.RECALLED);
        actionMap.put("re_submit", context -> ApprovalStatus.PENDING);
    }

    public boolean canTransition(ApprovalStatus currentStatus, ApprovalStatus targetStatus) {
        if (currentStatus == targetStatus) {
            return true;
        }
        Set<ApprovalStatus> allowedTransitions = transitionMap.get(currentStatus);
        return allowedTransitions != null && allowedTransitions.contains(targetStatus);
    }

    public Set<ApprovalStatus> getAvailableTransitions(ApprovalStatus currentStatus) {
        Set<ApprovalStatus> transitions = transitionMap.get(currentStatus);
        return transitions != null ? Collections.unmodifiableSet(transitions) : Collections.emptySet();
    }

    public ApprovalTransitionResult executeAction(ApprovalStatus currentStatus, String action, ApprovalTransitionContext context) {
        ApprovalTransitionAction transitionAction = actionMap.get(action);
        if (transitionAction == null) {
            return ApprovalTransitionResult.failed("Unknown action: " + action);
        }

        ApprovalStatus targetStatus = transitionAction.execute(context);
        if (!canTransition(currentStatus, targetStatus)) {
            return ApprovalTransitionResult.failed(
                String.format("Cannot transition from %s to %s", currentStatus, targetStatus)
            );
        }

        return ApprovalTransitionResult.success(currentStatus, targetStatus);
    }

    @FunctionalInterface
    public interface ApprovalTransitionAction {
        ApprovalStatus execute(ApprovalTransitionContext context);
    }

    public static class ApprovalTransitionContext {
        private Long instanceId;
        private Long operatorId;
        private String comments;
        private RejectTarget rejectTarget;
        private Long rejectNodeId;
        private SignType signType;
        private Long signUserId;
        private Map<String, Object> attributes = new HashMap<>();

        public ApprovalTransitionContext(Long instanceId, Long operatorId) {
            this.instanceId = instanceId;
            this.operatorId = operatorId;
        }

        public Long getInstanceId() { return instanceId; }
        public Long getOperatorId() { return operatorId; }
        public String getComments() { return comments; }
        public void setComments(String comments) { this.comments = comments; }
        public RejectTarget getRejectTarget() { return rejectTarget; }
        public void setRejectTarget(RejectTarget rejectTarget) { this.rejectTarget = rejectTarget; }
        public Long getRejectNodeId() { return rejectNodeId; }
        public void setRejectNodeId(Long rejectNodeId) { this.rejectNodeId = rejectNodeId; }
        public SignType getSignType() { return signType; }
        public void setSignType(SignType signType) { this.signType = signType; }
        public Long getSignUserId() { return signUserId; }
        public void setSignUserId(Long signUserId) { this.signUserId = signUserId; }
        public Map<String, Object> getAttributes() { return attributes; }
        public void setAttribute(String key, Object value) { this.attributes.put(key, value); }
    }

    public static class ApprovalTransitionResult {
        private final boolean success;
        private final ApprovalStatus fromStatus;
        private final ApprovalStatus toStatus;
        private final String message;

        private ApprovalTransitionResult(boolean success, ApprovalStatus fromStatus, ApprovalStatus toStatus, String message) {
            this.success = success;
            this.fromStatus = fromStatus;
            this.toStatus = toStatus;
            this.message = message;
        }

        public static ApprovalTransitionResult success(ApprovalStatus fromStatus, ApprovalStatus toStatus) {
            return new ApprovalTransitionResult(true, fromStatus, toStatus, null);
        }

        public static ApprovalTransitionResult failed(String message) {
            return new ApprovalTransitionResult(false, null, null, message);
        }

        public boolean isSuccess() { return success; }
        public ApprovalStatus getFromStatus() { return fromStatus; }
        public ApprovalStatus getToStatus() { return toStatus; }
        public String getMessage() { return message; }
    }
}
