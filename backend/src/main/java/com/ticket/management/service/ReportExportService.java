package com.ticket.management.service;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.write.style.column.LongestMatchColumnWidthStyleStrategy;
import com.ticket.management.entity.*;
import com.ticket.management.enums.TicketPriority;
import com.ticket.management.enums.TicketStatus;
import com.ticket.management.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.Predicate;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReportExportService {

    private static final Logger logger = LoggerFactory.getLogger(ReportExportService.class);

    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final ReportConfigRepository reportConfigRepository;

    public ReportExportService(
            TicketRepository ticketRepository,
            UserRepository userRepository,
            DepartmentRepository departmentRepository,
            ReportConfigRepository reportConfigRepository) {
        this.ticketRepository = ticketRepository;
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.reportConfigRepository = reportConfigRepository;
    }

    public byte[] exportTicketReport(ReportExportRequest request) throws IOException {
        List<Ticket> tickets = findTickets(request);
        
        List<TicketExcelData> dataList = tickets.stream()
            .map(this::convertToExcelData)
            .collect(Collectors.toList());

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        
        EasyExcel.write(outputStream, TicketExcelData.class)
            .registerWriteHandler(new LongestMatchColumnWidthStyleStrategy())
            .sheet("工单报表")
            .doWrite(dataList);

        logger.info("Exported ticket report with {} records", dataList.size());
        
        return outputStream.toByteArray();
    }

    public byte[] exportTicketReportByConfig(Long configId, Map<String, Object> parameters) throws IOException {
        ReportConfig config = reportConfigRepository.findById(configId)
            .orElseThrow(() -> new IllegalArgumentException("Report config not found: " + configId));

        ReportExportRequest request = parseConfigToRequest(config, parameters);
        return exportTicketReport(request);
    }

    public Map<String, Object> generateDashboardStatistics(Long departmentId, Long userId, LocalDateTime startDate, LocalDateTime endDate) {
        Map<String, Object> statistics = new LinkedHashMap<>();
        
        statistics.put("totalTickets", getTotalTickets(departmentId, userId, startDate, endDate));
        statistics.put("statusDistribution", getStatusDistribution(departmentId, userId, startDate, endDate));
        statistics.put("priorityDistribution", getPriorityDistribution(departmentId, userId, startDate, endDate));
        statistics.put("slaStatus", getSLAStatus(departmentId, userId, startDate, endDate));
        statistics.put("assigneeDistribution", getAssigneeDistribution(departmentId, startDate, endDate));
        statistics.put("dailyTrend", getDailyTrend(departmentId, userId, startDate, endDate));
        
        return statistics;
    }

    public Map<String, Object> generateSLAStatistics(Long departmentId, LocalDateTime startDate, LocalDateTime endDate) {
        Map<String, Object> statistics = new LinkedHashMap<>();
        
        List<Ticket> tickets = findTickets(new ReportExportRequest(
            departmentId, null, null, startDate, endDate, null, null
        ));

        long total = tickets.size();
        long onTime = tickets.stream()
            .filter(t -> Boolean.FALSE.equals(t.getSlaOverdue()))
            .count();
        long overdue = tickets.stream()
            .filter(t -> Boolean.TRUE.equals(t.getSlaOverdue()))
            .count();
        long warning = tickets.stream()
            .filter(t -> Boolean.TRUE.equals(t.getSlaWarningSent()) && Boolean.FALSE.equals(t.getSlaOverdue()))
            .count();

        statistics.put("total", total);
        statistics.put("onTime", onTime);
        statistics.put("overdue", overdue);
        statistics.put("warning", warning);
        statistics.put("onTimeRate", total > 0 ? (double) onTime / total : 0);
        
        return statistics;
    }

    private List<Ticket> findTickets(ReportExportRequest request) {
        Specification<Ticket> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (request.getDepartmentId() != null) {
                predicates.add(cb.equal(root.get("department").get("id"), request.getDepartmentId()));
            }

            if (request.getAssigneeId() != null) {
                predicates.add(cb.equal(root.get("assignee").get("id"), request.getAssigneeId()));
            }

            if (request.getStatus() != null && !request.getStatus().isEmpty()) {
                predicates.add(root.get("status").in(request.getStatus()));
            }

            if (request.getStartDate() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), request.getStartDate()));
            }

            if (request.getEndDate() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), request.getEndDate()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return ticketRepository.findAll(spec);
    }

    private TicketExcelData convertToExcelData(Ticket ticket) {
        TicketExcelData data = new TicketExcelData();
        data.setTicketNo(ticket.getTicketNo());
        data.setTitle(ticket.getTitle());
        data.setDescription(ticket.getDescription());
        data.setStatus(ticket.getStatus() != null ? ticket.getStatus().name() : "");
        data.setPriority(ticket.getPriority() != null ? ticket.getPriority().name() : "");
        data.setCreatorName(ticket.getCreator() != null ? ticket.getCreator().getRealName() : "");
        data.setAssigneeName(ticket.getAssignee() != null ? ticket.getAssignee().getRealName() : "");
        data.setDepartmentName(ticket.getDepartment() != null ? ticket.getDepartment().getName() : "");
        data.setCustomerName(ticket.getCustomerName());
        data.setCustomerPhone(ticket.getCustomerPhone());
        data.setCustomerEmail(ticket.getCustomerEmail());
        data.setSlaStatus(getSLAStatusText(ticket));
        data.setSlaDeadline(formatDateTime(ticket.getSlaDeadline()));
        data.setCreatedAt(formatDateTime(ticket.getCreatedAt()));
        data.setResponseTime(formatDateTime(ticket.getResponseTime()));
        data.setActualResolveTime(formatDateTime(ticket.getActualResolveTime()));
        data.setEscalationLevel(ticket.getEscalationLevel());
        data.setSource(ticket.getSource());
        data.setTags(ticket.getTags());
        return data;
    }

    private String getSLAStatusText(Ticket ticket) {
        if (Boolean.TRUE.equals(ticket.getSlaOverdue())) {
            return "已超时";
        }
        if (Boolean.TRUE.equals(ticket.getSlaWarningSent())) {
            return "即将超时";
        }
        return "正常";
    }

    private String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "";
        }
        return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    private Map<String, Long> getStatusDistribution(Long departmentId, Long userId, LocalDateTime startDate, LocalDateTime endDate) {
        Map<String, Long> distribution = new LinkedHashMap<>();
        for (TicketStatus status : TicketStatus.values()) {
            long count = countByStatus(departmentId, userId, status, startDate, endDate);
            if (count > 0) {
                distribution.put(status.name(), count);
            }
        }
        return distribution;
    }

    private Map<String, Long> getPriorityDistribution(Long departmentId, Long userId, LocalDateTime startDate, LocalDateTime endDate) {
        Map<String, Long> distribution = new LinkedHashMap<>();
        for (TicketPriority priority : TicketPriority.values()) {
            long count = countByPriority(departmentId, userId, priority, startDate, endDate);
            if (count > 0) {
                distribution.put(priority.name(), count);
            }
        }
        return distribution;
    }

    private Map<String, Long> getSLAStatus(Long departmentId, Long userId, LocalDateTime startDate, LocalDateTime endDate) {
        List<Ticket> tickets = findTickets(new ReportExportRequest(
            departmentId, userId, null, startDate, endDate, null, null
        ));

        Map<String, Long> statusMap = new LinkedHashMap<>();
        statusMap.put("正常", tickets.stream().filter(t -> !t.getSlaWarningSent() && !t.getSlaOverdue()).count());
        statusMap.put("预警", tickets.stream().filter(t -> t.getSlaWarningSent() && !t.getSlaOverdue()).count());
        statusMap.put("超时", tickets.stream().filter(t -> t.getSlaOverdue()).count());
        
        return statusMap;
    }

    private Map<String, Long> getAssigneeDistribution(Long departmentId, LocalDateTime startDate, LocalDateTime endDate) {
        List<Ticket> tickets = findTickets(new ReportExportRequest(
            departmentId, null, null, startDate, endDate, null, null
        ));

        return tickets.stream()
            .filter(t -> t.getAssignee() != null)
            .collect(Collectors.groupingBy(
                t -> t.getAssignee().getRealName(),
                LinkedHashMap::new,
                Collectors.counting()
            ));
    }

    private Map<String, Long> getDailyTrend(Long departmentId, Long userId, LocalDateTime startDate, LocalDateTime endDate) {
        List<Ticket> tickets = findTickets(new ReportExportRequest(
            departmentId, userId, null, startDate, endDate, null, null
        ));

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        return tickets.stream()
            .filter(t -> t.getCreatedAt() != null)
            .collect(Collectors.groupingBy(
                t -> t.getCreatedAt().format(formatter),
                TreeMap::new,
                Collectors.counting()
            ));
    }

    private long getTotalTickets(Long departmentId, Long userId, LocalDateTime startDate, LocalDateTime endDate) {
        return findTickets(new ReportExportRequest(
            departmentId, userId, null, startDate, endDate, null, null
        )).size();
    }

    private long countByStatus(Long departmentId, Long userId, TicketStatus status, LocalDateTime startDate, LocalDateTime endDate) {
        return findTickets(new ReportExportRequest(
            departmentId, userId, Collections.singletonList(status), startDate, endDate, null, null
        )).size();
    }

    private long countByPriority(Long departmentId, Long userId, TicketPriority priority, LocalDateTime startDate, LocalDateTime endDate) {
        List<Ticket> tickets = findTickets(new ReportExportRequest(
            departmentId, userId, null, startDate, endDate, null, null
        ));
        return tickets.stream()
            .filter(t -> t.getPriority() == priority)
            .count();
    }

    private ReportExportRequest parseConfigToRequest(ReportConfig config, Map<String, Object> parameters) {
        ReportExportRequest request = new ReportExportRequest();
        
        if (parameters != null) {
            if (parameters.containsKey("departmentId")) {
                request.setDepartmentId(Long.valueOf(parameters.get("departmentId").toString()));
            }
            if (parameters.containsKey("assigneeId")) {
                request.setAssigneeId(Long.valueOf(parameters.get("assigneeId").toString()));
            }
            if (parameters.containsKey("startDate")) {
                request.setStartDate(LocalDateTime.parse(parameters.get("startDate").toString()));
            }
            if (parameters.containsKey("endDate")) {
                request.setEndDate(LocalDateTime.parse(parameters.get("endDate").toString()));
            }
        }
        
        return request;
    }

    public static class TicketExcelData {
        @com.alibaba.excel.annotation.ExcelProperty("工单编号")
        private String ticketNo;
        
        @com.alibaba.excel.annotation.ExcelProperty("标题")
        private String title;
        
        @com.alibaba.excel.annotation.ExcelProperty("描述")
        private String description;
        
        @com.alibaba.excel.annotation.ExcelProperty("状态")
        private String status;
        
        @com.alibaba.excel.annotation.ExcelProperty("优先级")
        private String priority;
        
        @com.alibaba.excel.annotation.ExcelProperty("创建人")
        private String creatorName;
        
        @com.alibaba.excel.annotation.ExcelProperty("处理人")
        private String assigneeName;
        
        @com.alibaba.excel.annotation.ExcelProperty("所属部门")
        private String departmentName;
        
        @com.alibaba.excel.annotation.ExcelProperty("客户名称")
        private String customerName;
        
        @com.alibaba.excel.annotation.ExcelProperty("客户电话")
        private String customerPhone;
        
        @com.alibaba.excel.annotation.ExcelProperty("客户邮箱")
        private String customerEmail;
        
        @com.alibaba.excel.annotation.ExcelProperty("SLA状态")
        private String slaStatus;
        
        @com.alibaba.excel.annotation.ExcelProperty("SLA截止时间")
        private String slaDeadline;
        
        @com.alibaba.excel.annotation.ExcelProperty("创建时间")
        private String createdAt;
        
        @com.alibaba.excel.annotation.ExcelProperty("响应时间")
        private String responseTime;
        
        @com.alibaba.excel.annotation.ExcelProperty("解决时间")
        private String actualResolveTime;
        
        @com.alibaba.excel.annotation.ExcelProperty("升级级别")
        private Integer escalationLevel;
        
        @com.alibaba.excel.annotation.ExcelProperty("来源")
        private String source;
        
        @com.alibaba.excel.annotation.ExcelProperty("标签")
        private String tags;

        public String getTicketNo() { return ticketNo; }
        public void setTicketNo(String ticketNo) { this.ticketNo = ticketNo; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getPriority() { return priority; }
        public void setPriority(String priority) { this.priority = priority; }
        public String getCreatorName() { return creatorName; }
        public void setCreatorName(String creatorName) { this.creatorName = creatorName; }
        public String getAssigneeName() { return assigneeName; }
        public void setAssigneeName(String assigneeName) { this.assigneeName = assigneeName; }
        public String getDepartmentName() { return departmentName; }
        public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }
        public String getCustomerName() { return customerName; }
        public void setCustomerName(String customerName) { this.customerName = customerName; }
        public String getCustomerPhone() { return customerPhone; }
        public void setCustomerPhone(String customerPhone) { this.customerPhone = customerPhone; }
        public String getCustomerEmail() { return customerEmail; }
        public void setCustomerEmail(String customerEmail) { this.customerEmail = customerEmail; }
        public String getSlaStatus() { return slaStatus; }
        public void setSlaStatus(String slaStatus) { this.slaStatus = slaStatus; }
        public String getSlaDeadline() { return slaDeadline; }
        public void setSlaDeadline(String slaDeadline) { this.slaDeadline = slaDeadline; }
        public String getCreatedAt() { return createdAt; }
        public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
        public String getResponseTime() { return responseTime; }
        public void setResponseTime(String responseTime) { this.responseTime = responseTime; }
        public String getActualResolveTime() { return actualResolveTime; }
        public void setActualResolveTime(String actualResolveTime) { this.actualResolveTime = actualResolveTime; }
        public Integer getEscalationLevel() { return escalationLevel; }
        public void setEscalationLevel(Integer escalationLevel) { this.escalationLevel = escalationLevel; }
        public String getSource() { return source; }
        public void setSource(String source) { this.source = source; }
        public String getTags() { return tags; }
        public void setTags(String tags) { this.tags = tags; }
    }

    public static class ReportExportRequest {
        private Long departmentId;
        private Long assigneeId;
        private List<TicketStatus> status;
        private LocalDateTime startDate;
        private LocalDateTime endDate;
        private List<String> columns;
        private String sortBy;

        public ReportExportRequest() {}

        public ReportExportRequest(Long departmentId, Long assigneeId, List<TicketStatus> status,
                                   LocalDateTime startDate, LocalDateTime endDate, List<String> columns, String sortBy) {
            this.departmentId = departmentId;
            this.assigneeId = assigneeId;
            this.status = status;
            this.startDate = startDate;
            this.endDate = endDate;
            this.columns = columns;
            this.sortBy = sortBy;
        }

        public Long getDepartmentId() { return departmentId; }
        public void setDepartmentId(Long departmentId) { this.departmentId = departmentId; }
        public Long getAssigneeId() { return assigneeId; }
        public void setAssigneeId(Long assigneeId) { this.assigneeId = assigneeId; }
        public List<TicketStatus> getStatus() { return status; }
        public void setStatus(List<TicketStatus> status) { this.status = status; }
        public LocalDateTime getStartDate() { return startDate; }
        public void setStartDate(LocalDateTime startDate) { this.startDate = startDate; }
        public LocalDateTime getEndDate() { return endDate; }
        public void setEndDate(LocalDateTime endDate) { this.endDate = endDate; }
        public List<String> getColumns() { return columns; }
        public void setColumns(List<String> columns) { this.columns = columns; }
        public String getSortBy() { return sortBy; }
        public void setSortBy(String sortBy) { this.sortBy = sortBy; }
    }
}
