package com.ticket.management.entity;

import javax.persistence.*;

@Entity
@Table(name = "sla_rules")
public class SLARule extends BaseEntity {

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "code", unique = true, nullable = false, length = 50)
    private String code;

    @Column(name = "description", length = 255)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private TicketCategory category;

    @Column(name = "priority_level")
    private Integer priorityLevel;

    @Column(name = "response_minutes", nullable = false)
    private Integer responseMinutes;

    @Column(name = "resolution_minutes", nullable = false)
    private Integer resolutionMinutes;

    @Column(name = "warning_before_minutes", nullable = false)
    private Integer warningBeforeMinutes = 30;

    @Column(name = "escalation_level_1_minutes")
    private Integer escalationLevel1Minutes;

    @Column(name = "escalation_level_2_minutes")
    private Integer escalationLevel2Minutes;

    @Column(name = "escalation_level_3_minutes")
    private Integer escalationLevel3Minutes;

    @Column(name = "auto_escalate", nullable = false)
    private Boolean autoEscalate = false;

    @Column(name = "notify_assignee", nullable = false)
    private Boolean notifyAssignee = true;

    @Column(name = "notify_supervisor", nullable = false)
    private Boolean notifySupervisor = false;

    @Column(name = "notify_department_head", nullable = false)
    private Boolean notifyDepartmentHead = false;

    @Column(name = "working_hours_only", nullable = false)
    private Boolean workingHoursOnly = true;

    @Column(name = "is_default", nullable = false)
    private Boolean isDefault = false;

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

    public TicketCategory getCategory() {
        return category;
    }

    public void setCategory(TicketCategory category) {
        this.category = category;
    }

    public Integer getPriorityLevel() {
        return priorityLevel;
    }

    public void setPriorityLevel(Integer priorityLevel) {
        this.priorityLevel = priorityLevel;
    }

    public Integer getResponseMinutes() {
        return responseMinutes;
    }

    public void setResponseMinutes(Integer responseMinutes) {
        this.responseMinutes = responseMinutes;
    }

    public Integer getResolutionMinutes() {
        return resolutionMinutes;
    }

    public void setResolutionMinutes(Integer resolutionMinutes) {
        this.resolutionMinutes = resolutionMinutes;
    }

    public Integer getWarningBeforeMinutes() {
        return warningBeforeMinutes;
    }

    public void setWarningBeforeMinutes(Integer warningBeforeMinutes) {
        this.warningBeforeMinutes = warningBeforeMinutes;
    }

    public Integer getEscalationLevel1Minutes() {
        return escalationLevel1Minutes;
    }

    public void setEscalationLevel1Minutes(Integer escalationLevel1Minutes) {
        this.escalationLevel1Minutes = escalationLevel1Minutes;
    }

    public Integer getEscalationLevel2Minutes() {
        return escalationLevel2Minutes;
    }

    public void setEscalationLevel2Minutes(Integer escalationLevel2Minutes) {
        this.escalationLevel2Minutes = escalationLevel2Minutes;
    }

    public Integer getEscalationLevel3Minutes() {
        return escalationLevel3Minutes;
    }

    public void setEscalationLevel3Minutes(Integer escalationLevel3Minutes) {
        this.escalationLevel3Minutes = escalationLevel3Minutes;
    }

    public Boolean getAutoEscalate() {
        return autoEscalate;
    }

    public void setAutoEscalate(Boolean autoEscalate) {
        this.autoEscalate = autoEscalate;
    }

    public Boolean getNotifyAssignee() {
        return notifyAssignee;
    }

    public void setNotifyAssignee(Boolean notifyAssignee) {
        this.notifyAssignee = notifyAssignee;
    }

    public Boolean getNotifySupervisor() {
        return notifySupervisor;
    }

    public void setNotifySupervisor(Boolean notifySupervisor) {
        this.notifySupervisor = notifySupervisor;
    }

    public Boolean getNotifyDepartmentHead() {
        return notifyDepartmentHead;
    }

    public void setNotifyDepartmentHead(Boolean notifyDepartmentHead) {
        this.notifyDepartmentHead = notifyDepartmentHead;
    }

    public Boolean getWorkingHoursOnly() {
        return workingHoursOnly;
    }

    public void setWorkingHoursOnly(Boolean workingHoursOnly) {
        this.workingHoursOnly = workingHoursOnly;
    }

    public Boolean getDefault() {
        return isDefault;
    }

    public void setDefault(Boolean aDefault) {
        isDefault = aDefault;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }
}
