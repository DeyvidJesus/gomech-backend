package com.gomech.context;

import com.gomech.model.Organization;

/**
 * Thread-local context to store the current organization for the request
 */
public class OrganizationContext {
    
    private static final ThreadLocal<Organization> currentOrganization = new ThreadLocal<>();
    
    public static void setOrganization(Organization organization) {
        currentOrganization.set(organization);
    }
    
    public static Organization getOrganization() {
        return currentOrganization.get();
    }
    
    public static Long getOrganizationId() {
        Organization org = getOrganization();
        return org != null ? org.getId() : null;
    }
    
    public static void clear() {
        currentOrganization.remove();
    }
    
    public static boolean hasOrganization() {
        return currentOrganization.get() != null;
    }
}

