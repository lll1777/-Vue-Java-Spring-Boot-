package com.ticket.management.entity;

import com.ticket.management.enums.ApprovalStatus;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "approval_records")
public class ApprovalRecord extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instance_id", nullable = false)
    private ApprovalInstance instance;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "node_id", nullable = false)
    private ApprovalNode node;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ApprovalStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approver_id", nullable = false)
    private User approver;

    @Column(name = "comments", columnDefinition = "TEXT")
    private String comments;

    @Column(name = "action_at")
    private LocalDateTime actionAt;

    @Column(name = "is_forwarded", nullable = false)
    private Boolean isForwarded = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "forwarded_to_user_id")
    private User forwardedToUser;

    @Column(name = "forward_reason", length = 500)
    private String forwardReason;

    @Column(name = "order_index", nullable = false)
    private Integer orderIndex;

    public ApprovalInstance getInstance() {
        return instance;
    }

    public void setInstance(ApprovalInstance instance) {
        this.instance = instance;
    }

    public ApprovalNode getNode() {
        return node;
    }

    public void setNode(ApprovalNode node) {
        this.node = node;
    }

    public ApprovalStatus getStatus() {
        return status;
    }

    public void setStatus(ApprovalStatus status) {
        this.status = status;
    }

    public User getApprover() {
        return approver;
    }

    public void setApprover(User approver) {
        this.approver = approver;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public LocalDateTime getActionAt() {
        return actionAt;
    }

    public void setActionAt(LocalDateTime actionAt) {
        this.actionAt = actionAt;
    }

    public Boolean getForwarded() {
        return isForwarded;
    }

    public void setForwarded(Boolean forwarded) {
        isForwarded = forwarded;
    }

    public User getForwardedToUser() {
        return forwardedToUser;
    }

    public void setForwardedToUser(User forwardedToUser) {
        this.forwardedToUser = forwardedToUser;
    }

    public String getForwardReason() {
        return forwardReason;
    }

    public void setForwardReason(String forwardReason) {
        this.forwardReason = forwardReason;
    }

    public Integer getOrderIndex() {
        return orderIndex;
    }

    public void setOrderIndex(Integer orderIndex) {
        this.orderIndex = orderIndex;
    }
}
