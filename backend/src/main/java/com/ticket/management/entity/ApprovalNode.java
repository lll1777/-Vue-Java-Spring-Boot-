package com.ticket.management.entity;

import com.ticket.management.enums.ApprovalStatus;

import javax.persistence.*;

@Entity
@Table(name = "approval_nodes")
public class ApprovalNode extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "flow_id", nullable = false)
    private ApprovalFlow flow;

    @Column(name = "node_name", nullable = false, length = 100)
    private String nodeName;

    @Column(name = "node_code", nullable = false, length = 50)
    private String nodeCode;

    @Column(name = "order_index", nullable = false)
    private Integer orderIndex;

    @Column(name = "approver_type", nullable = false, length = 30)
    private String approverType;

    @Column(name = "approver_id")
    private Long approverId;

    @Column(name = "approver_role_id")
    private Long approverRoleId;

    @Column(name = "approver_department_id")
    private Long approverDepartmentId;

    @Column(name = "required_level")
    private Integer requiredLevel;

    @Column(name = "approval_type", nullable = false, length = 20)
    private String approvalType;

    @Column(name = "timeout_minutes")
    private Integer timeoutMinutes;

    @Column(name = "auto_approve_on_timeout", nullable = false)
    private Boolean autoApproveOnTimeout = false;

    @Column(name = "can_forward", nullable = false)
    private Boolean canForward = false;

    @Column(name = "can_reject", nullable = false)
    private Boolean canReject = true;

    @Column(name = "can_recall", nullable = false)
    private Boolean canRecall = false;

    @Column(name = "next_node_id")
    private Long nextNodeId;

    @Column(name = "reject_node_id")
    private Long rejectNodeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "default_status", length = 20)
    private ApprovalStatus defaultStatus = ApprovalStatus.PENDING;

    public ApprovalFlow getFlow() {
        return flow;
    }

    public void setFlow(ApprovalFlow flow) {
        this.flow = flow;
    }

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    public String getNodeCode() {
        return nodeCode;
    }

    public void setNodeCode(String nodeCode) {
        this.nodeCode = nodeCode;
    }

    public Integer getOrderIndex() {
        return orderIndex;
    }

    public void setOrderIndex(Integer orderIndex) {
        this.orderIndex = orderIndex;
    }

    public String getApproverType() {
        return approverType;
    }

    public void setApproverType(String approverType) {
        this.approverType = approverType;
    }

    public Long getApproverId() {
        return approverId;
    }

    public void setApproverId(Long approverId) {
        this.approverId = approverId;
    }

    public Long getApproverRoleId() {
        return approverRoleId;
    }

    public void setApproverRoleId(Long approverRoleId) {
        this.approverRoleId = approverRoleId;
    }

    public Long getApproverDepartmentId() {
        return approverDepartmentId;
    }

    public void setApproverDepartmentId(Long approverDepartmentId) {
        this.approverDepartmentId = approverDepartmentId;
    }

    public Integer getRequiredLevel() {
        return requiredLevel;
    }

    public void setRequiredLevel(Integer requiredLevel) {
        this.requiredLevel = requiredLevel;
    }

    public String getApprovalType() {
        return approvalType;
    }

    public void setApprovalType(String approvalType) {
        this.approvalType = approvalType;
    }

    public Integer getTimeoutMinutes() {
        return timeoutMinutes;
    }

    public void setTimeoutMinutes(Integer timeoutMinutes) {
        this.timeoutMinutes = timeoutMinutes;
    }

    public Boolean getAutoApproveOnTimeout() {
        return autoApproveOnTimeout;
    }

    public void setAutoApproveOnTimeout(Boolean autoApproveOnTimeout) {
        this.autoApproveOnTimeout = autoApproveOnTimeout;
    }

    public Boolean getCanForward() {
        return canForward;
    }

    public void setCanForward(Boolean canForward) {
        this.canForward = canForward;
    }

    public Boolean getCanReject() {
        return canReject;
    }

    public void setCanReject(Boolean canReject) {
        this.canReject = canReject;
    }

    public Boolean getCanRecall() {
        return canRecall;
    }

    public void setCanRecall(Boolean canRecall) {
        this.canRecall = canRecall;
    }

    public Long getNextNodeId() {
        return nextNodeId;
    }

    public void setNextNodeId(Long nextNodeId) {
        this.nextNodeId = nextNodeId;
    }

    public Long getRejectNodeId() {
        return rejectNodeId;
    }

    public void setRejectNodeId(Long rejectNodeId) {
        this.rejectNodeId = rejectNodeId;
    }

    public ApprovalStatus getDefaultStatus() {
        return defaultStatus;
    }

    public void setDefaultStatus(ApprovalStatus defaultStatus) {
        this.defaultStatus = defaultStatus;
    }
}
