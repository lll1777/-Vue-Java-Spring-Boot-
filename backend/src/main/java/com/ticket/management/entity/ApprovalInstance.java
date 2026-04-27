package com.ticket.management.entity;

import com.ticket.management.enums.ApprovalStatus;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "approval_instances")
public class ApprovalInstance extends BaseEntity {

    @Column(name = "instance_no", unique = true, nullable = false, length = 32)
    private String instanceNo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "flow_id", nullable = false)
    private ApprovalFlow flow;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ApprovalStatus status = ApprovalStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_node_id")
    private ApprovalNode currentNode;

    @Column(name = "current_node_index", nullable = false)
    private Integer currentNodeIndex = 0;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "initiator_id", nullable = false)
    private User initiator;

    @OneToMany(mappedBy = "instance", cascade = CascadeType.ALL)
    @OrderBy("createdAt ASC")
    private List<ApprovalRecord> records = new ArrayList<>();

    @Column(name = "comments", columnDefinition = "TEXT")
    private String comments;

    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;

    public String getInstanceNo() {
        return instanceNo;
    }

    public void setInstanceNo(String instanceNo) {
        this.instanceNo = instanceNo;
    }

    public ApprovalFlow getFlow() {
        return flow;
    }

    public void setFlow(ApprovalFlow flow) {
        this.flow = flow;
    }

    public Ticket getTicket() {
        return ticket;
    }

    public void setTicket(Ticket ticket) {
        this.ticket = ticket;
    }

    public ApprovalStatus getStatus() {
        return status;
    }

    public void setStatus(ApprovalStatus status) {
        this.status = status;
    }

    public ApprovalNode getCurrentNode() {
        return currentNode;
    }

    public void setCurrentNode(ApprovalNode currentNode) {
        this.currentNode = currentNode;
    }

    public Integer getCurrentNodeIndex() {
        return currentNodeIndex;
    }

    public void setCurrentNodeIndex(Integer currentNodeIndex) {
        this.currentNodeIndex = currentNodeIndex;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public User getInitiator() {
        return initiator;
    }

    public void setInitiator(User initiator) {
        this.initiator = initiator;
    }

    public List<ApprovalRecord> getRecords() {
        return records;
    }

    public void setRecords(List<ApprovalRecord> records) {
        this.records = records;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
