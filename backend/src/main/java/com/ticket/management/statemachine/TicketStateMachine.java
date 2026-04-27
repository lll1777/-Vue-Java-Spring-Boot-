package com.ticket.management.statemachine;

import com.ticket.management.enums.TicketStatus;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.*;

@Component
public class TicketStateMachine {

    private final Map<TicketStatus, Set<TicketStatus>> transitionMap = new EnumMap<>(TicketStatus.class);
    private final Map<String, TransitionAction> actionMap = new HashMap<>();

    @PostConstruct
    public void init() {
        initTransitions();
        initActions();
    }

    private void initTransitions() {
        addTransition(TicketStatus.DRAFT, TicketStatus.PENDING_APPROVAL);
        addTransition(TicketStatus.DRAFT, TicketStatus.CANCELLED);

        addTransition(TicketStatus.PENDING_APPROVAL, TicketStatus.APPROVED);
        addTransition(TicketStatus.PENDING_APPROVAL, TicketStatus.REJECTED);
        addTransition(TicketStatus.PENDING_APPROVAL, TicketStatus.CANCELLED);

        addTransition(TicketStatus.REJECTED, TicketStatus.DRAFT);
        addTransition(TicketStatus.REJECTED, TicketStatus.CANCELLED);

        addTransition(TicketStatus.APPROVED, TicketStatus.ASSIGNED);
        addTransition(TicketStatus.APPROVED, TicketStatus.CANCELLED);

        addTransition(TicketStatus.ASSIGNED, TicketStatus.IN_PROGRESS);
        addTransition(TicketStatus.ASSIGNED, TicketStatus.PENDING_APPROVAL);
        addTransition(TicketStatus.ASSIGNED, TicketStatus.ESCALATED);

        addTransition(TicketStatus.IN_PROGRESS, TicketStatus.PENDING_REVIEW);
        addTransition(TicketStatus.IN_PROGRESS, TicketStatus.ASSIGNED);
        addTransition(TicketStatus.IN_PROGRESS, TicketStatus.ESCALATED);
        addTransition(TicketStatus.IN_PROGRESS, TicketStatus.CANCELLED);

        addTransition(TicketStatus.ESCALATED, TicketStatus.ASSIGNED);
        addTransition(TicketStatus.ESCALATED, TicketStatus.IN_PROGRESS);
        addTransition(TicketStatus.ESCALATED, TicketStatus.CANCELLED);

        addTransition(TicketStatus.PENDING_REVIEW, TicketStatus.RESOLVED);
        addTransition(TicketStatus.PENDING_REVIEW, TicketStatus.IN_PROGRESS);
        addTransition(TicketStatus.PENDING_REVIEW, TicketStatus.ESCALATED);

        addTransition(TicketStatus.RESOLVED, TicketStatus.CLOSED);
        addTransition(TicketStatus.RESOLVED, TicketStatus.IN_PROGRESS);

        addTransition(TicketStatus.CLOSED, TicketStatus.IN_PROGRESS);
    }

    private void addTransition(TicketStatus from, TicketStatus to) {
        transitionMap.computeIfAbsent(from, k -> EnumSet.noneOf(TicketStatus.class)).add(to);
    }

    private void initActions() {
        actionMap.put("submit", (context) -> {
            return TicketStatus.PENDING_APPROVAL;
        });

        actionMap.put("approve", (context) -> {
            return TicketStatus.APPROVED;
        });

        actionMap.put("reject", (context) -> {
            return TicketStatus.REJECTED;
        });

        actionMap.put("assign", (context) -> {
            return TicketStatus.ASSIGNED;
        });

        actionMap.put("start", (context) -> {
            return TicketStatus.IN_PROGRESS;
        });

        actionMap.put("submit_review", (context) -> {
            return TicketStatus.PENDING_REVIEW;
        });

        actionMap.put("resolve", (context) -> {
            return TicketStatus.RESOLVED;
        });

        actionMap.put("close", (context) -> {
            return TicketStatus.CLOSED;
        });

        actionMap.put("cancel", (context) -> {
            return TicketStatus.CANCELLED;
        });

        actionMap.put("escalate", (context) -> {
            return TicketStatus.ESCALATED;
        });

        actionMap.put("reopen", (context) -> {
            return TicketStatus.IN_PROGRESS;
        });

        actionMap.put("modify", (context) -> {
            return TicketStatus.DRAFT;
        });
    }

    public boolean canTransition(TicketStatus currentStatus, TicketStatus targetStatus) {
        if (currentStatus == targetStatus) {
            return true;
        }
        Set<TicketStatus> allowedTransitions = transitionMap.get(currentStatus);
        return allowedTransitions != null && allowedTransitions.contains(targetStatus);
    }

    public Set<TicketStatus> getAvailableTransitions(TicketStatus currentStatus) {
        Set<TicketStatus> transitions = transitionMap.get(currentStatus);
        return transitions != null ? Collections.unmodifiableSet(transitions) : Collections.emptySet();
    }

    public TransitionResult executeAction(TicketStatus currentStatus, String action, TransitionContext context) {
        TransitionAction transitionAction = actionMap.get(action);
        if (transitionAction == null) {
            return TransitionResult.failed("Unknown action: " + action);
        }

        TicketStatus targetStatus = transitionAction.execute(context);
        if (!canTransition(currentStatus, targetStatus)) {
            return TransitionResult.failed(
                String.format("Cannot transition from %s to %s", currentStatus, targetStatus)
            );
        }

        return TransitionResult.success(currentStatus, targetStatus);
    }

    @FunctionalInterface
    public interface TransitionAction {
        TicketStatus execute(TransitionContext context);
    }

    public static class TransitionContext {
        private Long ticketId;
        private Long operatorId;
        private String comments;
        private Map<String, Object> attributes = new HashMap<>();

        public TransitionContext(Long ticketId, Long operatorId) {
            this.ticketId = ticketId;
            this.operatorId = operatorId;
        }

        public Long getTicketId() {
            return ticketId;
        }

        public Long getOperatorId() {
            return operatorId;
        }

        public String getComments() {
            return comments;
        }

        public void setComments(String comments) {
            this.comments = comments;
        }

        public Map<String, Object> getAttributes() {
            return attributes;
        }

        public void setAttribute(String key, Object value) {
            this.attributes.put(key, value);
        }

        public Object getAttribute(String key) {
            return this.attributes.get(key);
        }
    }

    public static class TransitionResult {
        private final boolean success;
        private final TicketStatus fromStatus;
        private final TicketStatus toStatus;
        private final String message;

        private TransitionResult(boolean success, TicketStatus fromStatus, TicketStatus toStatus, String message) {
            this.success = success;
            this.fromStatus = fromStatus;
            this.toStatus = toStatus;
            this.message = message;
        }

        public static TransitionResult success(TicketStatus fromStatus, TicketStatus toStatus) {
            return new TransitionResult(true, fromStatus, toStatus, null);
        }

        public static TransitionResult failed(String message) {
            return new TransitionResult(false, null, null, message);
        }

        public boolean isSuccess() {
            return success;
        }

        public TicketStatus getFromStatus() {
            return fromStatus;
        }

        public TicketStatus getToStatus() {
            return toStatus;
        }

        public String getMessage() {
            return message;
        }
    }
}
