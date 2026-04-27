package com.ticket.management.concurrency;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Service
public class DistributedLockService {

    private static final Logger logger = LoggerFactory.getLogger(DistributedLockService.class);

    private final RedissonClient redissonClient;

    public DistributedLockService(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    public <T> T executeWithLock(String lockKey, long waitTime, long leaseTime, TimeUnit unit, Supplier<T> supplier) {
        RLock lock = redissonClient.getLock(lockKey);
        
        try {
            boolean acquired = lock.tryLock(waitTime, leaseTime, unit);
            if (!acquired) {
                throw new LockAcquisitionException("Failed to acquire lock: " + lockKey);
            }
            
            logger.debug("Acquired lock: {}", lockKey);
            return supplier.get();
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LockAcquisitionException("Lock acquisition interrupted: " + lockKey, e);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
                logger.debug("Released lock: {}", lockKey);
            }
        }
    }

    public void executeWithLock(String lockKey, long waitTime, long leaseTime, TimeUnit unit, Runnable runnable) {
        executeWithLock(lockKey, waitTime, leaseTime, unit, () -> {
            runnable.run();
            return null;
        });
    }

    public <T> T executeWithTicketLock(Long ticketId, Supplier<T> supplier) {
        String lockKey = "ticket:" + ticketId;
        return executeWithLock(lockKey, 3, 30, TimeUnit.SECONDS, supplier);
    }

    public void executeWithTicketLock(Long ticketId, Runnable runnable) {
        executeWithTicketLock(ticketId, () -> {
            runnable.run();
            return null;
        });
    }

    public <T> T executeWithApprovalLock(Long instanceId, Supplier<T> supplier) {
        String lockKey = "approval:" + instanceId;
        return executeWithLock(lockKey, 3, 30, TimeUnit.SECONDS, supplier);
    }

    public boolean isLocked(String lockKey) {
        RLock lock = redissonClient.getLock(lockKey);
        return lock.isLocked();
    }

    public void forceUnlock(String lockKey) {
        RLock lock = redissonClient.getLock(lockKey);
        if (lock.isLocked()) {
            lock.forceUnlock();
            logger.warn("Force unlocked: {}", lockKey);
        }
    }

    public static class LockAcquisitionException extends RuntimeException {
        public LockAcquisitionException(String message) {
            super(message);
        }

        public LockAcquisitionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
