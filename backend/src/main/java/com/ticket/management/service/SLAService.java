package com.ticket.management.service;

import com.ticket.management.entity.*;
import com.ticket.management.enums.SLAStatus;
import com.ticket.management.enums.TicketStatus;
import com.ticket.management.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service
public class SLAService {

    private static final Logger logger = LoggerFactory.getLogger(SLAService.class);

    private static final List<TicketStatus> ACTIVE_STATUSES = Arrays.asList(
        TicketStatus.ASSIGNED,
        TicketStatus.IN_PROGRESS,
        TicketStatus.PENDING_REVIEW,
        TicketStatus.ESCALATED
    );

    private final TicketRepository ticketRepository;
    private final SLARuleRepository slaRuleRepository;
    private final SLAWarningRepository slaWarningRepository;

    @Value("${sla.warning-before-minutes:30}")
    private int defaultWarningBeforeMinutes;

    public SLAService(
            TicketRepository ticketRepository,
            SLARuleRepository slaRuleRepository,
            SLAWarningRepository slaWarningRepository) {
        this.ticketRepository = ticketRepository;
        this.slaRuleRepository = slaRuleRepository;
        this.slaWarningRepository = slaWarningRepository;
    }

    @Scheduled(fixedDelayString = "${sla.check-interval-seconds:60}000")
    @Transactional
    public void checkSLA() {
        logger.info("Starting SLA check at {}", LocalDateTime.now());

        checkWarnings();
        checkOverdue();

        logger.info("SLA check completed at {}", LocalDateTime.now());
    }

    private void checkWarnings() {
        LocalDateTime warningTime = LocalDateTime.now().plusMinutes(defaultWarningBeforeMinutes);
        
        List<Ticket> warningTickets = ticketRepository.findTicketsForSlaWarning(
            ACTIVE_STATUSES,
            warningTime
        );

        for (Ticket ticket : warningTickets) {
            processWarning(ticket);
        }
    }

    private void checkOverdue() {
        LocalDateTime now = LocalDateTime.now();
        
        List<Ticket> overdueTickets = ticketRepository.findOverdueTickets(
            ACTIVE_STATUSES,
            now
        );

        for (Ticket ticket : overdueTickets) {
            processOverdue(ticket);
        }
    }

    private void processWarning(Ticket ticket) {
        if (ticket.getSlaWarningSent()) {
            return;
        }

        SLAWarning warning = new SLAWarning();
        warning.setTicket(ticket);
        warning.setStatus(SLAStatus.WARNING);
        warning.setWarningType("即将超时");
        warning.setMessage(String.format("工单 [%s] 将在 %d 分钟内超时", 
            ticket.getTicketNo(), defaultWarningBeforeMinutes));
        warning.setSlaDeadline(ticket.getSlaDeadline());
        warning.setTriggeredAt(LocalDateTime.now());
        warning.setNotified(false);
        
        slaWarningRepository.save(warning);

        ticket.setSlaWarningSent(true);
        ticketRepository.save(ticket);

        logger.warn("SLA Warning: Ticket {} will be overdue in {} minutes", 
            ticket.getTicketNo(), defaultWarningBeforeMinutes);

        sendNotification(ticket, SLAStatus.WARNING);
    }

    private void processOverdue(Ticket ticket) {
        if (ticket.getSlaOverdue()) {
            if (shouldEscalate(ticket)) {
                escalateTicket(ticket);
            }
            return;
        }

        SLAWarning warning = new SLAWarning();
        warning.setTicket(ticket);
        warning.setStatus(SLAStatus.OVERDUE);
        warning.setWarningType("已超时");
        warning.setMessage(String.format("工单 [%s] 已超时，SLA截止时间: %s", 
            ticket.getTicketNo(), ticket.getSlaDeadline()));
        warning.setSlaDeadline(ticket.getSlaDeadline());
        warning.setTriggeredAt(LocalDateTime.now());
        warning.setNotified(false);
        
        slaWarningRepository.save(warning);

        ticket.setSlaOverdue(true);
        ticket.setSlaViolationCount(ticket.getSlaViolationCount() + 1);
        ticketRepository.save(ticket);

        logger.error("SLA Overdue: Ticket {} is now overdue. SLA deadline was: {}", 
            ticket.getTicketNo(), ticket.getSlaDeadline());

        sendNotification(ticket, SLAStatus.OVERDUE);
    }

    private boolean shouldEscalate(Ticket ticket) {
        SLARule rule = findSLARule(ticket);
        if (rule == null || !rule.getAutoEscalate()) {
            return false;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime slaDeadline = ticket.getSlaDeadline();
        if (slaDeadline == null) {
            return false;
        }

        long minutesOverdue = java.time.Duration.between(slaDeadline, now).toMinutes();
        int currentLevel = ticket.getEscalationLevel();

        if (currentLevel == 0 && rule.getEscalationLevel1Minutes() != null 
            && minutesOverdue >= rule.getEscalationLevel1Minutes()) {
            return true;
        }
        if (currentLevel == 1 && rule.getEscalationLevel2Minutes() != null 
            && minutesOverdue >= rule.getEscalationLevel2Minutes()) {
            return true;
        }
        if (currentLevel == 2 && rule.getEscalationLevel3Minutes() != null 
            && minutesOverdue >= rule.getEscalationLevel3Minutes()) {
            return true;
        }

        return false;
    }

    private void escalateTicket(Ticket ticket) {
        int newLevel = ticket.getEscalationLevel() + 1;
        if (newLevel > 3) {
            return;
        }

        ticket.setEscalationLevel(newLevel);
        ticket.setEscalatedAt(LocalDateTime.now());
        ticket.setStatus(TicketStatus.ESCALATED);
        ticketRepository.save(ticket);

        SLAWarning warning = new SLAWarning();
        warning.setTicket(ticket);
        warning.setStatus(SLAStatus.OVERDUE);
        warning.setWarningType("升级告警");
        warning.setMessage(String.format("工单 [%s] 已升级到级别 %d", ticket.getTicketNo(), newLevel));
        warning.setSlaDeadline(ticket.getSlaDeadline());
        warning.setTriggeredAt(LocalDateTime.now());
        warning.setNotified(false);
        
        slaWarningRepository.save(warning);

        logger.warn("SLA Escalation: Ticket {} escalated to level {}", 
            ticket.getTicketNo(), newLevel);

        sendEscalationNotification(ticket, newLevel);
    }

    private SLARule findSLARule(Ticket ticket) {
        TicketCategory category = ticket.getCategory();
        Integer priorityLevel = ticket.getPriority() != null ? ticket.getPriority().getLevel() : null;
        Long categoryId = category != null ? category.getId() : null;

        List<SLARule> rules = slaRuleRepository.findMatchingRules(categoryId, priorityLevel);
        
        if (!rules.isEmpty()) {
            return rules.get(0);
        }

        return slaRuleRepository.findByIsDefaultTrueAndEnabledTrue()
            .orElse(null);
    }

    private void sendNotification(Ticket ticket, SLAStatus status) {
        SLARule rule = findSLARule(ticket);
        if (rule == null) {
            return;
        }

        if (rule.getNotifyAssignee() && ticket.getAssignee() != null) {
            sendToUser(ticket.getAssignee(), ticket, status);
        }

        if (rule.getNotifySupervisor() && ticket.getAssignee() != null) {
            sendToSupervisor(ticket.getAssignee(), ticket, status);
        }

        if (rule.getNotifyDepartmentHead() && ticket.getDepartment() != null) {
            sendToDepartmentHead(ticket.getDepartment(), ticket, status);
        }
    }

    private void sendEscalationNotification(Ticket ticket, int level) {
        SLARule rule = findSLARule(ticket);
        if (rule == null) {
            return;
        }

        if (ticket.getDepartment() != null) {
            sendToDepartmentHead(ticket.getDepartment(), ticket, SLAStatus.OVERDUE);
        }
    }

    private void sendToUser(User user, Ticket ticket, SLAStatus status) {
        logger.info("Sending SLA {} notification to user {} for ticket {}", 
            status, user.getUsername(), ticket.getTicketNo());
    }

    private void sendToSupervisor(User user, Ticket ticket, SLAStatus status) {
        logger.info("Sending SLA {} notification to supervisor of user {} for ticket {}", 
            status, user.getUsername(), ticket.getTicketNo());
    }

    private void sendToDepartmentHead(Department department, Ticket ticket, SLAStatus status) {
        logger.info("Sending SLA {} notification to department head of {} for ticket {}", 
            status, department.getName(), ticket.getTicketNo());
    }

    public SLAStatus getTicketSLAStatus(Ticket ticket) {
        if (ticket.getSlaDeadline() == null) {
            return SLAStatus.NORMAL;
        }

        LocalDateTime now = LocalDateTime.now();
        
        if (ticket.getSlaOverdue()) {
            return SLAStatus.OVERDUE;
        }
        
        if (ticket.getSlaWarningSent()) {
            return SLAStatus.WARNING;
        }

        LocalDateTime warningTime = now.plusMinutes(defaultWarningBeforeMinutes);
        if (ticket.getSlaDeadline().isBefore(warningTime)) {
            return SLAStatus.WARNING;
        }

        if (ticket.getSlaDeadline().isBefore(now)) {
            return SLAStatus.OVERDUE;
        }

        return SLAStatus.NORMAL;
    }

    public java.time.Duration getRemainingTime(Ticket ticket) {
        if (ticket.getSlaDeadline() == null) {
            return null;
        }
        
        LocalDateTime now = LocalDateTime.now();
        if (ticket.getSlaDeadline().isBefore(now)) {
            return java.time.Duration.ZERO;
        }
        
        return java.time.Duration.between(now, ticket.getSlaDeadline());
    }
}
