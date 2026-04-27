package com.ticket.management.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticket.management.entity.Department;
import com.ticket.management.entity.Ticket;
import com.ticket.management.entity.User;
import com.ticket.management.repository.TicketRepository;
import com.ticket.management.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.util.UrlPathHelper;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class ApiPermissionInterceptor implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(ApiPermissionInterceptor.class);

    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    private static final Set<String> ADMIN_ROLES = new HashSet<>(Arrays.asList(
        "ROLE_ADMIN",
        "ROLE_SUPER_ADMIN",
        "ROLE_SYSTEM_ADMIN"
    ));

    private static final Set<String> ADMIN_ONLY_PATHS = new HashSet<>(Arrays.asList(
        "/api/batch",
        "/api/system",
        "/api/report/config",
        "/api/approval/config"
    ));

    private static final Set<String> BATCH_OPERATION_PATHS = new HashSet<>(Arrays.asList(
        "/api/batch/assign",
        "/api/batch/status",
        "/api/batch/close",
        "/api/batch/resolve",
        "/api/batch/assign/async"
    ));

    private static final Pattern TICKET_ID_PATTERN = Pattern.compile("/api/tickets/(\\d+)");
    private static final Pattern TICKET_ACTION_PATTERN = Pattern.compile("/api/tickets/(\\d+)/(assign|status|history)");

    private static final UrlPathHelper URL_PATH_HELPER = new UrlPathHelper();

    @Autowired
    public ApiPermissionInterceptor(
            TicketRepository ticketRepository,
            UserRepository userRepository,
            ObjectMapper objectMapper) {
        this.ticketRepository = ticketRepository;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) 
            throws Exception {
        
        String requestUri = URL_PATH_HELPER.getPathWithinApplication(request);
        String method = request.getMethod();

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            sendErrorResponse(response, HttpStatus.UNAUTHORIZED, "未认证，请登录");
            return false;
        }

        String username = authentication.getName();
        User currentUser = userRepository.findByUsername(username).orElse(null);
        if (currentUser == null) {
            sendErrorResponse(response, HttpStatus.UNAUTHORIZED, "用户不存在");
            return false;
        }

        boolean isAdmin = isAdminUser(authentication);
        if (isAdmin) {
            logger.debug("Admin user {} accessing path: {}", username, requestUri);
            return true;
        }

        if (isAdminOnlyPath(requestUri)) {
            logger.warn("Non-admin user {} attempted to access admin-only path: {}", username, requestUri);
            sendErrorResponse(response, HttpStatus.FORBIDDEN, 
                "权限不足：该功能仅管理员可访问");
            return false;
        }

        if (isBatchOperationPath(requestUri)) {
            logger.warn("Non-admin user {} attempted to access batch operation: {}", username, requestUri);
            sendErrorResponse(response, HttpStatus.FORBIDDEN, 
                "权限不足：批量处理功能仅管理员可使用");
            return false;
        }

        Long ticketId = extractTicketId(requestUri);
        if (ticketId != null) {
            Ticket ticket = ticketRepository.findById(ticketId).orElse(null);
            if (ticket == null) {
                return true;
            }

            String action = extractTicketAction(requestUri);
            if ("GET".equalsIgnoreCase(method) && action == null) {
                if (!canViewTicket(currentUser, ticket)) {
                    logger.warn("User {} attempted to view unauthorized ticket: {}. User dept: {}, Ticket dept: {}", 
                        username, ticketId, currentUser.getDepartment(), ticket.getDepartment());
                    sendErrorResponse(response, HttpStatus.FORBIDDEN, 
                        "权限不足：您无权查看该工单");
                    return false;
                }
            } else if ("PUT".equalsIgnoreCase(method) || "POST".equalsIgnoreCase(method)) {
                if (!canModifyTicket(currentUser, ticket)) {
                    logger.warn("User {} attempted to modify unauthorized ticket: {}", username, ticketId);
                    sendErrorResponse(response, HttpStatus.FORBIDDEN, 
                        "权限不足：您无权修改该工单");
                    return false;
                }
            }
        }

        return true;
    }

    private boolean isAdminUser(Authentication authentication) {
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .anyMatch(ADMIN_ROLES::contains);
    }

    private boolean isAdminOnlyPath(String path) {
        return ADMIN_ONLY_PATHS.stream()
            .anyMatch(prefix -> path.startsWith(prefix));
    }

    private boolean isBatchOperationPath(String path) {
        return BATCH_OPERATION_PATHS.contains(path);
    }

    private Long extractTicketId(String path) {
        Matcher matcher = TICKET_ID_PATTERN.matcher(path);
        if (matcher.find()) {
            try {
                return Long.parseLong(matcher.group(1));
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    private String extractTicketAction(String path) {
        Matcher matcher = TICKET_ACTION_PATTERN.matcher(path);
        if (matcher.find()) {
            return matcher.group(2);
        }
        return null;
    }

    public boolean canViewTicket(User currentUser, Ticket ticket) {
        Long currentUserId = currentUser.getId();
        Department currentUserDept = currentUser.getDepartment();
        Long currentUserDeptId = currentUserDept != null ? currentUserDept.getId() : null;

        Long ticketCreatorId = ticket.getCreator() != null ? ticket.getCreator().getId() : null;
        Long ticketAssigneeId = ticket.getAssignee() != null ? ticket.getAssignee().getId() : null;
        Long ticketDeptId = ticket.getDepartment() != null ? ticket.getDepartment().getId() : null;

        if (currentUserId.equals(ticketCreatorId)) {
            return true;
        }

        if (currentUserId.equals(ticketAssigneeId)) {
            return true;
        }

        if (currentUserDeptId != null && currentUserDeptId.equals(ticketDeptId)) {
            return true;
        }

        return false;
    }

    public boolean canModifyTicket(User currentUser, Ticket ticket) {
        if (!canViewTicket(currentUser, ticket)) {
            return false;
        }

        Long currentUserId = currentUser.getId();
        Long ticketCreatorId = ticket.getCreator() != null ? ticket.getCreator().getId() : null;
        Long ticketAssigneeId = ticket.getAssignee() != null ? ticket.getAssignee().getId() : null;

        if (currentUserId.equals(ticketCreatorId)) {
            return true;
        }

        if (currentUserId.equals(ticketAssigneeId)) {
            return true;
        }

        Department currentUserDept = currentUser.getDepartment();
        Department ticketDept = ticket.getDepartment();
        if (currentUserDept != null && ticketDept != null && 
            currentUserDept.getId().equals(ticketDept.getId())) {
            Integer userLevel = currentUser.getLevel();
            return userLevel != null && userLevel >= 2;
        }

        return false;
    }

    private void sendErrorResponse(HttpServletResponse response, HttpStatus status, String message) 
            throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        Map<String, Object> errorResponse = new LinkedHashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("code", status.value());
        errorResponse.put("message", message);
        errorResponse.put("timestamp", new Date());

        String json = objectMapper.writeValueAsString(errorResponse);
        response.getWriter().write(json);
    }
}
