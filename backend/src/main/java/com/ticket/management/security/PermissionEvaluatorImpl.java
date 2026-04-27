package com.ticket.management.security;

import com.ticket.management.entity.Permission;
import com.ticket.management.entity.Role;
import com.ticket.management.entity.User;
import com.ticket.management.repository.UserRepository;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class PermissionEvaluatorImpl implements PermissionEvaluator {

    private final UserRepository userRepository;

    public PermissionEvaluatorImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) {
        if (authentication == null || permission == null) {
            return false;
        }

        String username = authentication.getName();
        return userRepository.findByUsername(username)
            .map(user -> hasPermission(user, permission.toString()))
            .orElse(false);
    }

    @Override
    public boolean hasPermission(Authentication authentication, Serializable targetId, String targetType, Object permission) {
        if (authentication == null || permission == null) {
            return false;
        }

        String username = authentication.getName();
        return userRepository.findByUsername(username)
            .map(user -> hasPermissionWithLevel(user, permission.toString(), targetId, targetType))
            .orElse(false);
    }

    private boolean hasPermission(User user, String permissionCode) {
        Set<Permission> userPermissions = user.getRoles().stream()
            .flatMap(role -> role.getPermissions().stream())
            .collect(Collectors.toSet());

        return userPermissions.stream()
            .anyMatch(p -> p.getCode().equals(permissionCode) && p.getEnabled());
    }

    private boolean hasPermissionWithLevel(User user, String permissionCode, Serializable targetId, String targetType) {
        if (!hasPermission(user, permissionCode)) {
            return false;
        }

        Set<Permission> userPermissions = user.getRoles().stream()
            .flatMap(role -> role.getPermissions().stream())
            .collect(Collectors.toSet());

        Integer userMaxLevel = user.getRoles().stream()
            .mapToInt(Role::getLevel)
            .max()
            .orElse(1);

        return userPermissions.stream()
            .filter(p -> p.getCode().equals(permissionCode))
            .anyMatch(p -> p.getRequiredLevel() <= userMaxLevel);
    }

    public boolean hasLevel(User user, Integer requiredLevel) {
        Integer userMaxLevel = user.getRoles().stream()
            .mapToInt(Role::getLevel)
            .max()
            .orElse(1);
        return userMaxLevel >= requiredLevel;
    }

    public boolean hasRole(User user, String roleCode) {
        return user.getRoles().stream()
            .anyMatch(role -> role.getCode().equals(roleCode));
    }

    public Set<String> getUserPermissionCodes(User user) {
        return user.getRoles().stream()
            .flatMap(role -> role.getPermissions().stream())
            .filter(Permission::getEnabled)
            .map(Permission::getCode)
            .collect(Collectors.toSet());
    }

    public Integer getUserMaxLevel(User user) {
        return user.getRoles().stream()
            .mapToInt(Role::getLevel)
            .max()
            .orElse(1);
    }
}
