package com.gomech.model;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public enum Role {
    ADMIN("ADMIN"),
    USER("USER");

    private final String role;

    Role(String role) {
        this.role = role;
    }

    public String getRole() {
        return role;
    }

    /**
     * Retorna as authorities do Spring Security para este role.
     * ADMIN recebe ROLE_ADMIN e ROLE_USER (herda permissões de USER)
     * USER recebe apenas ROLE_USER
     */
    public Collection<? extends GrantedAuthority> getAuthorities() {
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        
        // Adiciona a authority principal do role
        authorities.add(new SimpleGrantedAuthority("ROLE_" + this.name()));
        
        // ADMIN herda permissões de USER
        if (this == ADMIN) {
            authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        }
        
        return authorities;
    }

    /**
     * Verifica se este role é ADMIN
     */
    public boolean isAdmin() {
        return this == ADMIN;
    }

    /**
     * Verifica se este role é USER
     */
    public boolean isUser() {
        return this == USER;
    }
}
