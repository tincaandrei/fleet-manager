package com.fleet.auth.service;

import com.fleet.auth.entity.Credential;
import com.fleet.auth.entity.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

@RequiredArgsConstructor
public class CredentialDetails implements UserDetails {

    private final Credential credential;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Role.toAuthorities(getRole());
    }

    public Role getRole() {
        return credential.getRole().getRoleName();
    }

    public Long getUserId() {
        return credential.getUserData() == null ? null : credential.getUserData().getUserId();
    }

    @Override
    public String getPassword() {
        return credential.getPasswordHash();
    }

    @Override
    public String getUsername() {
        return credential.getUsername();
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
        return true;
    }
}
