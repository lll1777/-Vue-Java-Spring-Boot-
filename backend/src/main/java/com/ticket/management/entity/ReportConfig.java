package com.ticket.management.entity;

import javax.persistence.*;

@Entity
@Table(name = "report_configs")
public class ReportConfig extends BaseEntity {

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "code", unique = true, nullable = false, length = 50)
    private String code;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "report_type", nullable = false, length = 50)
    private String reportType;

    @Column(name = "data_source", nullable = false, length = 100)
    private String dataSource;

    @Column(name = "query_config", columnDefinition = "TEXT")
    private String queryConfig;

    @Column(name = "column_config", columnDefinition = "TEXT")
    private String columnConfig;

    @Column(name = "filter_config", columnDefinition = "TEXT")
    private String filterConfig;

    @Column(name = "sort_config", columnDefinition = "TEXT")
    private String sortConfig;

    @Column(name = "chart_config", columnDefinition = "TEXT")
    private String chartConfig;

    @Column(name = "export_format", length = 20)
    private String exportFormat = "EXCEL";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private User owner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    @Column(name = "is_public", nullable = false)
    private Boolean isPublic = false;

    @Column(name = "is_system", nullable = false)
    private Boolean isSystem = false;

    @Column(name = "enabled", nullable = false)
    private Boolean enabled = true;

    @Column(name = "last_executed_at")
    private java.time.LocalDateTime lastExecutedAt;

    @Column(name = "execution_count", nullable = false)
    private Integer executionCount = 0;

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

    public String getReportType() {
        return reportType;
    }

    public void setReportType(String reportType) {
        this.reportType = reportType;
    }

    public String getDataSource() {
        return dataSource;
    }

    public void setDataSource(String dataSource) {
        this.dataSource = dataSource;
    }

    public String getQueryConfig() {
        return queryConfig;
    }

    public void setQueryConfig(String queryConfig) {
        this.queryConfig = queryConfig;
    }

    public String getColumnConfig() {
        return columnConfig;
    }

    public void setColumnConfig(String columnConfig) {
        this.columnConfig = columnConfig;
    }

    public String getFilterConfig() {
        return filterConfig;
    }

    public void setFilterConfig(String filterConfig) {
        this.filterConfig = filterConfig;
    }

    public String getSortConfig() {
        return sortConfig;
    }

    public void setSortConfig(String sortConfig) {
        this.sortConfig = sortConfig;
    }

    public String getChartConfig() {
        return chartConfig;
    }

    public void setChartConfig(String chartConfig) {
        this.chartConfig = chartConfig;
    }

    public String getExportFormat() {
        return exportFormat;
    }

    public void setExportFormat(String exportFormat) {
        this.exportFormat = exportFormat;
    }

    public User getOwner() {
        return owner;
    }

    public void setOwner(User owner) {
        this.owner = owner;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public Boolean getPublic() {
        return isPublic;
    }

    public void setPublic(Boolean aPublic) {
        isPublic = aPublic;
    }

    public Boolean getSystem() {
        return isSystem;
    }

    public void setSystem(Boolean system) {
        isSystem = system;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public java.time.LocalDateTime getLastExecutedAt() {
        return lastExecutedAt;
    }

    public void setLastExecutedAt(java.time.LocalDateTime lastExecutedAt) {
        this.lastExecutedAt = lastExecutedAt;
    }

    public Integer getExecutionCount() {
        return executionCount;
    }

    public void setExecutionCount(Integer executionCount) {
        this.executionCount = executionCount;
    }
}
