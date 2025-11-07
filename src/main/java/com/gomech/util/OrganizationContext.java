package com.gomech.util;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.gomech.model.User;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;

/**
 * Utilitário para extrair o organization_id do contexto da requisição.
 * Suporta múltiplas fontes:
 * 1. Header HTTP: X-Organization-ID
 * 2. JWT Token: claim organizationId
 * 3. Usuário autenticado: user.getOrganization().getId()
 */
@Component
public class OrganizationContext {

    /**
     * Obtém o organization_id da requisição atual.
     * Tenta em ordem: Header → JWT → Usuário autenticado
     * 
     * @return Optional com o organizationId, ou Optional.empty() se não encontrado
     */
    public Optional<Long> getCurrentOrganizationId() {
        // 1. Tentar obter do header HTTP
        Optional<Long> fromHeader = getFromHeader();
        if (fromHeader.isPresent()) {
            return fromHeader;
        }

        // 2. Tentar obter do JWT token
        Optional<Long> fromToken = getFromJwtToken();
        if (fromToken.isPresent()) {
            return fromToken;
        }

        // 3. Tentar obter do usuário autenticado
        return getFromAuthenticatedUser();
    }

    /**
     * Obtém o organization_id do header HTTP X-Organization-ID
     */
    public Optional<Long> getFromHeader() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) {
                return Optional.empty();
            }

            HttpServletRequest request = attributes.getRequest();
            String orgIdHeader = request.getHeader("X-Organization-ID");

            if (orgIdHeader != null && !orgIdHeader.isEmpty()) {
                return Optional.of(Long.parseLong(orgIdHeader));
            }
        } catch (NumberFormatException e) {
            // Header presente mas não é um número válido
        } catch (Exception e) {
            // Outro erro ao acessar o header
        }

        return Optional.empty();
    }

    /**
     * Obtém o organization_id do JWT token (claim organizationId)
     */
    public Optional<Long> getFromJwtToken() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) {
                return Optional.empty();
            }

            HttpServletRequest request = attributes.getRequest();
            String authHeader = request.getHeader("Authorization");

            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                DecodedJWT jwt = JWT.decode(token);
                
                Long orgId = jwt.getClaim("organizationId").asLong();
                if (orgId != null) {
                    return Optional.of(orgId);
                }
            }
        } catch (Exception e) {
            // Token inválido ou claim não presente
        }

        return Optional.empty();
    }

    /**
     * Obtém o organization_id do usuário autenticado no SecurityContext
     */
    public Optional<Long> getFromAuthenticatedUser() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            
            if (authentication != null && authentication.isAuthenticated() 
                && authentication.getPrincipal() instanceof User user) {
                
                if (user.getOrganization() != null && user.getOrganization().getId() != null) {
                    return Optional.of(user.getOrganization().getId());
                }
            }
        } catch (Exception e) {
            // Erro ao acessar o SecurityContext
        }

        return Optional.empty();
    }

    /**
     * Obtém o organization_id ou lança exceção se não encontrado
     * 
     * @return organizationId
     * @throws IllegalStateException se o organizationId não for encontrado
     */
    public Long getRequiredOrganizationId() {
        return getCurrentOrganizationId()
            .orElseThrow(() -> new IllegalStateException(
                "Organization ID não encontrado. Certifique-se de que o usuário está autenticado e possui uma organização."
            ));
    }
}

