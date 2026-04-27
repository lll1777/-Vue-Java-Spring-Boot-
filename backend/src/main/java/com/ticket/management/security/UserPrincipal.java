package com.ticket.management.security;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

public class UserPrincipal implements UserDetails {

    private Long id;
    private String username;
    private String realName;
    private String email;
    private Integer level;
    private Long departmentId;
    
    @JsonIgnore
    private String password;
    
    private Collection<? extends GrantedAuthority> authorities;
    private boolean enabled;

    private UserPrincipal(Builder builder) {
        this.id = builder.id;
        this.username = builder.username;
        this.realName = builder.realName;
        this.email = builder.email;
        this.level = builder.level;
        this.departmentId = builder.departmentId;
        this.password = builder.password;
        this.authorities = builder.authorities != null ? builder.authorities : Collections.emptyList();
        this.enabled = builder.enabled;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Long getId() {
        return id;
    }

    public String getRealName() {
        return realName;
    }

    public String getEmail() {
        return email;
    }

    public Integer getLevel() {
        return level;
    }

    public Long getDepartmentId() {
        return departmentId;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    public static class Builder {
        private Long id;
        private String username;
        private String realName;
        private String email;
        private Integer level;
        private Long departmentId;
        private String password;
        private Collection<? extends GrantedAuthority> authorities;
        private boolean enabled = true;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder username(String username) {
            this.username = username;
            return this;
        }

        public Builder realName(String realName) {
            this.realName = realName;
            return this;
        }

        public Builder email(String email) {
            this.email = email;
            return this;
        }

        public Builder level(Integer level) {
            this.level = level;
            return this;
        }

        public Builder departmentId(Long departmentId) {
            this.departmentId = departmentId;
            return this;
        }

        public Builder password(String password) {
            this.password = password;
            return this;
        }

        public Builder authorities(Collection<? extends GrantedAuthority> authorities) {
            this.authorities = authorities;
            return this;
        }

        public Builder enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public UserPrincipal build() {
            return new UserPrincipal(this);
        }
    }
}
