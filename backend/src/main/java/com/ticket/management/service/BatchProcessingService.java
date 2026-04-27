package com.ticket.management.service;

import com.ticket.management.concurrency.DistributedLockService;
import com.ticket.management.entity.Ticket;
import com.ticket.management.entity.User;
import com.ticket.management.enums.TicketStatus;
import com.ticket.management.repository.TicketRepository;
import com.ticket.management.repository.UserRepository;
import com.ticket.management.statemachine.TicketStateMachine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Service
public class BatchProcessingService {

    private static final Logger logger = LoggerFactory.getLogger(BatchProcessingService.class);

    private static final int BATCH_SIZE = 100;
    private static final int THREAD_POOL_SIZE = 4;

    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final TicketStateMachine ticketStateMachine;
    private final DistributedLockService distributedLockService;
    
    private final ExecutorService executorService;

    public BatchProcessingService(
            TicketRepository ticketRepository,
            UserRepository userRepository,
            TicketStateMachine ticketStateMachine,
            DistributedLockService distributedLockService) {
        this.ticketRepository = ticketRepository;
        this.userRepository = userRepository;
        this.ticketStateMachine = ticketStateMachine;
        this.distributedLockService = distributedLockService;
        this.executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
    }

    @Transactional
    public BatchResult batchAssign(List<Long> ticketIds, Long assigneeId, Long operatorId, String comments) {
        User operator = userRepository.findById(operatorId)
            .orElseThrow(() -> new IllegalArgumentException("Operator not found: " + operatorId));
        
        User assignee = userRepository.findById(assigneeId)
            .orElseThrow(() -> new IllegalArgumentException("Assignee not found: " + assigneeId));

        List<Long> successIds = new ArrayList<>();
        List<Long> failedIds = new ArrayList<>();
        List<String> errorMessages = new ArrayList<>();

        List<List<Long>> batches = partitionList(ticketIds, BATCH_SIZE);

        for (List<Long> batch : batches) {
            for (Long ticketId : batch) {
                try {
                    assignSingleTicket(ticketId, assignee, operator, comments);
                    successIds.add(ticketId);
                } catch (Exception e) {
                    failedIds.add(ticketId);
                    errorMessages.add("Ticket " + ticketId + ": " + e.getMessage());
                    logger.error("Failed to assign ticket {}: {}", ticketId, e.getMessage());
                }
            }
        }

        logger.info("Batch assign completed: success={}, failed={}", successIds.size(), failedIds.size());
        
        return new BatchResult(successIds, failedIds, errorMessages);
    }

    @Transactional
    public BatchResult batchUpdateStatus(List<Long> ticketIds, String action, Long operatorId, String comments) {
        User operator = userRepository.findById(operatorId)
            .orElseThrow(() -> new IllegalArgumentException("Operator not found: " + operatorId));

        List<Long> successIds = new ArrayList<>();
        List<Long> failedIds = new ArrayList<>();
        List<String> errorMessages = new ArrayList<>();

        List<List<Long>> batches = partitionList(ticketIds, BATCH_SIZE);

        for (List<Long> batch : batches) {
            for (Long ticketId : batch) {
                try {
                    updateSingleTicketStatus(ticketId, action, operator, comments);
                    successIds.add(ticketId);
                } catch (Exception e) {
                    failedIds.add(ticketId);
                    errorMessages.add("Ticket " + ticketId + ": " + e.getMessage());
                    logger.error("Failed to update status for ticket {}: {}", ticketId, e.getMessage());
                }
            }
        }

        logger.info("Batch status update completed: success={}, failed={}", successIds.size(), failedIds.size());
        
        return new BatchResult(successIds, failedIds, errorMessages);
    }

    @Transactional
    public BatchResult batchClose(List<Long> ticketIds, Long operatorId, String comments) {
        return batchUpdateStatus(ticketIds, "close", operatorId, comments);
    }

    @Transactional
    public BatchResult batchResolve(List<Long> ticketIds, Long operatorId, String comments) {
        return batchUpdateStatus(ticketIds, "resolve", operatorId, comments);
    }

    public BatchResult batchAssignAsync(List<Long> ticketIds, Long assigneeId, Long operatorId, String comments) {
        User operator = userRepository.findById(operatorId)
            .orElseThrow(() -> new IllegalArgumentException("Operator not found: " + operatorId));
        
        User assignee = userRepository.findById(assigneeId)
            .orElseThrow(() -> new IllegalArgumentException("Assignee not found: " + assigneeId));

        List<CompletableFuture<Long>> futures = ticketIds.stream()
            .map(ticketId -> CompletableFuture.supplyAsync(() -> {
                try {
                    assignSingleTicket(ticketId, assignee, operator, comments);
                    return ticketId;
                } catch (Exception e) {
                    logger.error("Async batch assign failed for ticket {}: {}", ticketId, e.getMessage());
                    return -1L;
                }
            }, executorService))
            .collect(Collectors.toList());

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        List<Long> successIds = new ArrayList<>();
        List<Long> failedIds = new ArrayList<>();

        for (CompletableFuture<Long> future : futures) {
            try {
                Long result = future.get();
                if (result > 0) {
                    successIds.add(result);
                } else {
                    failedIds.add(Math.abs(result));
                }
            } catch (Exception e) {
                logger.error("Error getting future result: {}", e.getMessage());
            }
        }

        logger.info("Async batch assign completed: success={}, failed={}", successIds.size(), failedIds.size());
        
        return new BatchResult(successIds, failedIds, new ArrayList<>());
    }

    private void assignSingleTicket(Long ticketId, User assignee, User operator, String comments) {
        distributedLockService.executeWithTicketLock(ticketId, () -> {
            Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found: " + ticketId));

            TicketStatus currentStatus = ticket.getStatus();
            TicketStatus newStatus = TicketStatus.ASSIGNED;

            if (!ticketStateMachine.canTransition(currentStatus, newStatus)) {
                throw new IllegalStateException(
                    String.format("Cannot transition ticket from %s to %s", currentStatus, newStatus)
                );
            }

            ticket.setAssignee(assignee);
            ticket.setStatus(newStatus);
            
            if (ticket.getResponseTime() == null) {
                ticket.setResponseTime(LocalDateTime.now());
            }

            ticketRepository.save(ticket);

            logger.info("Assigned ticket {} to {} by {}", ticketId, assignee.getUsername(), operator.getUsername());
        });
    }

    private void updateSingleTicketStatus(Long ticketId, String action, User operator, String comments) {
        distributedLockService.executeWithTicketLock(ticketId, () -> {
            Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found: " + ticketId));

            TicketStateMachine.TransitionContext context = new TicketStateMachine.TransitionContext(ticketId, operator.getId());
            context.setComments(comments);

            TicketStateMachine.TransitionResult result = ticketStateMachine.executeAction(
                ticket.getStatus(), action, context
            );

            if (!result.isSuccess()) {
                throw new IllegalStateException("State transition failed: " + result.getMessage());
            }

            ticket.setStatus(result.getToStatus());

            if (result.getToStatus() == TicketStatus.RESOLVED) {
                ticket.setActualResolveTime(LocalDateTime.now());
            }

            if (result.getToStatus() == TicketStatus.IN_PROGRESS && ticket.getResponseTime() == null) {
                ticket.setResponseTime(LocalDateTime.now());
            }

            ticketRepository.save(ticket);

            logger.info("Updated ticket {} status from {} to {} by {}", 
                ticketId, result.getFromStatus(), result.getToStatus(), operator.getUsername());
        });
    }

    private <T> List<List<T>> partitionList(List<T> list, int batchSize) {
        List<List<T>> batches = new ArrayList<>();
        for (int i = 0; i < list.size(); i += batchSize) {
            batches.add(list.subList(i, Math.min(i + batchSize, list.size())));
        }
        return batches;
    }

    public void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public static class BatchResult {
        private final List<Long> successIds;
        private final List<Long> failedIds;
        private final List<String> errorMessages;

        public BatchResult(List<Long> successIds, List<Long> failedIds, List<String> errorMessages) {
            this.successIds = successIds;
            this.failedIds = failedIds;
            this.errorMessages = errorMessages;
        }

        public List<Long> getSuccessIds() {
            return successIds;
        }

        public List<Long> getFailedIds() {
            return failedIds;
        }

        public List<String> getErrorMessages() {
            return errorMessages;
        }

        public int getSuccessCount() {
            return successIds.size();
        }

        public int getFailedCount() {
            return failedIds.size();
        }

        public int getTotalCount() {
            return successIds.size() + failedIds.size();
        }
    }
}
