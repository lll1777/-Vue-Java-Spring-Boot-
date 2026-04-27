package com.ticket.management.entity;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "approval_flows")
public class ApprovalFlow extends BaseEntity {

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "code", unique = true, nullable = false, length = 50)
    private String code;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "flow_type", nullable = false, length = 50)
    private String flowType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trigger_category_id")
    private TicketCategory triggerCategory;

    @Column(name = "trigger_priority_from")
    private Integer triggerPriorityFrom;

    @Column(name = "trigger_priority_to")
    private Integer triggerPriorityTo;

    @Column(name = "trigger_department_id")
    private Long triggerDepartmentId;

    @OneToMany(mappedBy = "flow", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orderIndex ASC")
    private List<ApprovalNode> nodes = new ArrayList<>();

    @Column(name = "version", nullable = false)
    private Integer version = 1;

    @Column(name = "enabled", nullable = false)
    private Boolean enabled = true;

    @Column(name = "is_default", nullable = false)
    private Boolean isDefault = false;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getFlowType() {
        return flowType;
    }

    public void setFlowType(String flowType) {
        this.flowType = flowType;
    }

    public TicketCategory getTriggerCategory() {
        return triggerCategory;
    }

    public void setTriggerCategory(TicketCategory triggerCategory) {
        this.triggerCategory = triggerCategory;
    }

    public Integer getTriggerPriorityFrom() {
        return triggerPriorityFrom;
    }

    public void setTriggerPriorityFrom(Integer triggerPriorityFrom) {
        this.triggerPriorityFrom = triggerPriorityFrom;
    }

    public Integer getTriggerPriorityTo() {
        return triggerPriorityTo;
    }

    public void setTriggerPriorityTo(Integer triggerPriorityTo) {
        this.triggerPriorityTo = triggerPriorityTo;
    }

    public Long getTriggerDepartmentId() {
        return triggerDepartmentId;
    }

    public void setTriggerDepartmentId(Long triggerDepartmentId) {
        this.triggerDepartmentId = triggerDepartmentId;
    }

    public List<ApprovalNode> getNodes() {
        return nodes;
    }

    public void setNodes(List<ApprovalNode> nodes) {
        this.nodes = nodes;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Boolean getDefault() {
        return isDefault;
    }

    public void setDefault(Boolean aDefault) {
        isDefault = aDefault;
    }
}
