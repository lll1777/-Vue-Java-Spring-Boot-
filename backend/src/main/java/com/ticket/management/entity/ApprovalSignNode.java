package com.ticket.management.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "approval_sign_nodes")
public class ApprovalSignNode extends BaseEntity {

    public enum SignType {
        BEFORE_SIGN,
        AFTER_SIGN,
        PARALLEL_SIGN
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instance_id", nullable = false)
    private ApprovalInstance instance;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "original_node_id", nullable = false)
    private ApprovalNode originalNode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sign_user_id", nullable = false)
    private User signUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "operator_id", nullable = false)
    private User operator;

    @Column(name = "sign_type", nullable = false, length = 20)
    private String signType;

    @Column(name = "reason", length = 500)
    private String reason;

    @Column(name = "comments", length = 500)
    private String comments;

    @Column(name = "active", nullable = false)
    private Boolean active = true;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    public ApprovalInstance getInstance() { return instance; }
    public void setInstance(ApprovalInstance instance) { this.instance = instance; }
    public ApprovalNode getOriginalNode() { return originalNode; }
    public void setOriginalNode(ApprovalNode originalNode) { this.originalNode = originalNode; }
    public User getSignUser() { return signUser; }
    public void setSignUser(User signUser) { this.signUser = signUser; }
    public User getOperator() { return operator; }
    public void setOperator(User operator) { this.operator = operator; }
    public String getSignType() { return signType; }
    public void setSignType(String signType) { this.signType = signType; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getComments() { return comments; }
    public void setComments(String comments) { this.comments = comments; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getApprovedAt() { return approvedAt; }
    public void setApprovedAt(LocalDateTime approvedAt) { this.approvedAt = approvedAt; }
}
