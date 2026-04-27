package com.ticket.management.entity;

import com.ticket.management.enums.TicketPriority;
import com.ticket.management.enums.TicketStatus;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tickets", indexes = {
    @Index(name = "idx_ticket_no", columnList = "ticket_no"),
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_creator", columnList = "creator_id"),
    @Index(name = "idx_assignee", columnList = "assignee_id"),
    @Index(name = "idx_department", columnList = "department_id"),
    @Index(name = "idx_created_at", columnList = "created_at")
})
public class Ticket extends BaseEntity {

    @Column(name = "ticket_no", unique = true, nullable = false, length = 32)
    private String ticketNo;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private TicketStatus status = TicketStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 20)
    private TicketPriority priority = TicketPriority.MEDIUM;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", nullable = false)
    private User creator;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_id")
    private User assignee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private TicketCategory category;

    @Column(name = "customer_name", length = 100)
    private String customerName;

    @Column(name = "customer_phone", length = 20)
    private String customerPhone;

    @Column(name = "customer_email", length = 100)
    private String customerEmail;

    @Column(name = "expected_resolve_time")
    private LocalDateTime expectedResolveTime;

    @Column(name = "actual_resolve_time")
    private LocalDateTime actualResolveTime;

    @Column(name = "response_time")
    private LocalDateTime responseTime;

    @Column(name = "sla_deadline")
    private LocalDateTime slaDeadline;

    @Column(name = "sla_warning_sent", nullable = false)
    private Boolean slaWarningSent = false;

    @Column(name = "sla_overdue", nullable = false)
    private Boolean slaOverdue = false;

    @Column(name = "sla_violation_count", nullable = false)
    private Integer slaViolationCount = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_ticket_id")
    private Ticket parentTicket;

    @OneToMany(mappedBy = "parentTicket")
    private List<Ticket> childTickets = new ArrayList<>();

    @Column(name = "escalation_level", nullable = false)
    private Integer escalationLevel = 0;

    @Column(name = "escalated_at")
    private LocalDateTime escalatedAt;

    @Column(name = "source", length = 50)
    private String source;

    @Column(name = "tags", length = 500)
    private String tags;

    @Column(name = "remarks", columnDefinition = "TEXT")
    private String remarks;

    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;

    public String getTicketNo() {
        return ticketNo;
    }

    public void setTicketNo(String ticketNo) {
        this.ticketNo = ticketNo;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public TicketStatus getStatus() {
        return status;
    }

    public void setStatus(TicketStatus status) {
        this.status = status;
    }

    public TicketPriority getPriority() {
        return priority;
    }

    public void setPriority(TicketPriority priority) {
        this.priority = priority;
    }

    public User getCreator() {
        return creator;
    }

    public void setCreator(User creator) {
        this.creator = creator;
    }

    public User getAssignee() {
        return assignee;
    }

    public void setAssignee(User assignee) {
        this.assignee = assignee;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public TicketCategory getCategory() {
        return category;
    }

    public void setCategory(TicketCategory category) {
        this.category = category;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCustomerPhone() {
        return customerPhone;
    }

    public void setCustomerPhone(String customerPhone) {
        this.customerPhone = customerPhone;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public void setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
    }

    public LocalDateTime getExpectedResolveTime() {
        return expectedResolveTime;
    }

    public void setExpectedResolveTime(LocalDateTime expectedResolveTime) {
        this.expectedResolveTime = expectedResolveTime;
    }

    public LocalDateTime getActualResolveTime() {
        return actualResolveTime;
    }

    public void setActualResolveTime(LocalDateTime actualResolveTime) {
        this.actualResolveTime = actualResolveTime;
    }

    public LocalDateTime getResponseTime() {
        return responseTime;
    }

    public void setResponseTime(LocalDateTime responseTime) {
        this.responseTime = responseTime;
    }

    public LocalDateTime getSlaDeadline() {
        return slaDeadline;
    }

    public void setSlaDeadline(LocalDateTime slaDeadline) {
        this.slaDeadline = slaDeadline;
    }

    public Boolean getSlaWarningSent() {
        return slaWarningSent;
    }

    public void setSlaWarningSent(Boolean slaWarningSent) {
        this.slaWarningSent = slaWarningSent;
    }

    public Boolean getSlaOverdue() {
        return slaOverdue;
    }

    public void setSlaOverdue(Boolean slaOverdue) {
        this.slaOverdue = slaOverdue;
    }

    public Integer getSlaViolationCount() {
        return slaViolationCount;
    }

    public void setSlaViolationCount(Integer slaViolationCount) {
        this.slaViolationCount = slaViolationCount;
    }

    public Ticket getParentTicket() {
        return parentTicket;
    }

    public void setParentTicket(Ticket parentTicket) {
        this.parentTicket = parentTicket;
    }

    public List<Ticket> getChildTickets() {
        return childTickets;
    }

    public void setChildTickets(List<Ticket> childTickets) {
        this.childTickets = childTickets;
    }

    public Integer getEscalationLevel() {
        return escalationLevel;
    }

    public void setEscalationLevel(Integer escalationLevel) {
        this.escalationLevel = escalationLevel;
    }

    public LocalDateTime getEscalatedAt() {
        return escalatedAt;
    }

    public void setEscalatedAt(LocalDateTime escalatedAt) {
        this.escalatedAt = escalatedAt;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
