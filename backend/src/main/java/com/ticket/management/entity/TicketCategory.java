package com.ticket.management.entity;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "ticket_categories")
public class TicketCategory extends BaseEntity {

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "code", unique = true, nullable = false, length = 50)
    private String code;

    @Column(name = "description", length = 255)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private TicketCategory parent;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL)
    private Set<TicketCategory> children = new HashSet<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "default_department_id")
    private Department defaultDepartment;

    @Column(name = "default_priority", length = 20)
    private String defaultPriority = "MEDIUM";

    @Column(name = "default_sla_hours")
    private Integer defaultSlaHours = 24;

    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    @Column(name = "enabled", nullable = false)
    private Boolean enabled = true;

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

    public TicketCategory getParent() {
        return parent;
    }

    public void setParent(TicketCategory parent) {
        this.parent = parent;
    }

    public Set<TicketCategory> getChildren() {
        return children;
    }

    public void setChildren(Set<TicketCategory> children) {
        this.children = children;
    }

    public Department getDefaultDepartment() {
        return defaultDepartment;
    }

    public void setDefaultDepartment(Department defaultDepartment) {
        this.defaultDepartment = defaultDepartment;
    }

    public String getDefaultPriority() {
        return defaultPriority;
    }

    public void setDefaultPriority(String defaultPriority) {
        this.defaultPriority = defaultPriority;
    }

    public Integer getDefaultSlaHours() {
        return defaultSlaHours;
    }

    public void setDefaultSlaHours(Integer defaultSlaHours) {
        this.defaultSlaHours = defaultSlaHours;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }
}
