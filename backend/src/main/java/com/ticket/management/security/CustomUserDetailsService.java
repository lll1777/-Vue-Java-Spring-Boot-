package com.ticket.management.security;

import com.ticket.management.entity.User;
import com.ticket.management.repository.UserRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> 
                new UsernameNotFoundException("User not found with username: " + username)
            );

        return buildUserPrincipal(user);
    }

    @Transactional
    public UserDetails loadUserById(Long id) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> 
                new UsernameNotFoundException("User not found with id: " + id)
            );

        return buildUserPrincipal(user);
    }

    private UserPrincipal buildUserPrincipal(User user) {
        List<GrantedAuthority> authorities = user.getRoles().stream()
            .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getCode()))
            .collect(Collectors.toList());

        user.getRoles().stream()
            .flatMap(role -> role.getPermissions().stream())
            .forEach(permission -> 
                authorities.add(new SimpleGrantedAuthority(permission.getCode()))
            );

        return UserPrincipal.builder()
            .id(user.getId())
            .username(user.getUsername())
            .password(user.getPassword())
            .realName(user.getRealName())
            .email(user.getEmail())
            .level(user.getLevel())
            .departmentId(user.getDepartment() != null ? user.getDepartment().getId() : null)
            .enabled(user.getActive())
            .authorities(authorities)
            .build();
    }
}
