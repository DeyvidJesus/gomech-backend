package com.gomech.configuration;

import com.gomech.context.OrganizationContext;
import com.gomech.model.Organization;
import com.gomech.model.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

/**
 * Interceptor to extract and set the organization from the authenticated user
 */
@Slf4j
@Component
public class OrganizationInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication != null && authentication.isAuthenticated() && authentication.getPrincipal() instanceof User) {
            User user = (User) authentication.getPrincipal();
            Organization organization = user.getOrganization();
            
            if (organization != null) {
                OrganizationContext.setOrganization(organization);
                log.debug("Organization set in context: {} (ID: {})", organization.getName(), organization.getId());
            } else {
                log.warn("User {} has no organization assigned", user.getEmail());
            }
        }
        
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        // Nothing to do here
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // Clear the context after the request is complete
        OrganizationContext.clear();
        log.debug("Organization context cleared");
    }
}

