package com.ticket.management.entity;

import com.ticket.management.enums.TicketStatus;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ticket_histories")
public class TicketHistory extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;

    @Enumerated(EnumType.STRING)
    @Column(name = "old_status", length = 30)
    private TicketStatus oldStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", length = 30)
    private TicketStatus newStatus;

    @Column(name = "action", nullable = false, length = 50)
    private String action;

    @Column(name = "action_description", length = 500)
    private String actionDescription;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "operator_id")
    private User operator;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "old_assignee_id")
    private User oldAssignee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "new_assignee_id")
    private User newAssignee;

    @Column(name = "change_content", columnDefinition = "TEXT")
    private String changeContent;

    @Column(name = "remarks", columnDefinition = "TEXT")
    private String remarks;

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "action_time")
    private LocalDateTime actionTime;

    public Ticket getTicket() {
        return ticket;
    }

    public void setTicket(Ticket ticket) {
        this.ticket = ticket;
    }

    public TicketStatus getOldStatus() {
        return oldStatus;
    }

    public void setOldStatus(TicketStatus oldStatus) {
        this.oldStatus = oldStatus;
    }

    public TicketStatus getNewStatus() {
        return newStatus;
    }

    public void setNewStatus(TicketStatus newStatus) {
        this.newStatus = newStatus;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getActionDescription() {
        return actionDescription;
    }

    public void setActionDescription(String actionDescription) {
        this.actionDescription = actionDescription;
    }

    public User getOperator() {
        return operator;
    }

    public void setOperator(User operator) {
        this.operator = operator;
    }

    public User getOldAssignee() {
        return oldAssignee;
    }

    public void setOldAssignee(User oldAssignee) {
        this.oldAssignee = oldAssignee;
    }

    public User getNewAssignee() {
        return newAssignee;
    }

    public void setNewAssignee(User newAssignee) {
        this.newAssignee = newAssignee;
    }

    public String getChangeContent() {
        return changeContent;
    }

    public void setChangeContent(String changeContent) {
        this.changeContent = changeContent;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public LocalDateTime getActionTime() {
        return actionTime;
    }

    public void setActionTime(LocalDateTime actionTime) {
        this.actionTime = actionTime;
    }
}
