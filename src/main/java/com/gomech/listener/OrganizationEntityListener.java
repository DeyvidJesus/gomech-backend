package com.gomech.listener;

import com.gomech.context.OrganizationContext;
import com.gomech.model.Organization;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;

/**
 * JPA EntityListener que automaticamente popula o campo 'organization' 
 * em todas as entidades antes de persist e update.
 * 
 * Para usar, adicione nas entidades:
 * @EntityListeners(OrganizationEntityListener.class)
 */
@Slf4j
public class OrganizationEntityListener {

    @PrePersist
    public void setOrganizationOnPersist(Object entity) {
        setOrganizationIfNeeded(entity, "persist");
    }

    @PreUpdate
    public void setOrganizationOnUpdate(Object entity) {
        // Não sobrescrever organization em updates, apenas garantir que existe
        try {
            Field organizationField = findOrganizationField(entity.getClass());
            if (organizationField != null) {
                organizationField.setAccessible(true);
                Object currentOrg = organizationField.get(entity);
                
                // Só seta se ainda estiver null
                if (currentOrg == null) {
                    setOrganizationIfNeeded(entity, "update");
                }
            }
        } catch (Exception e) {
            log.error("Erro ao verificar organization no update: {}", e.getMessage());
        }
    }

    private void setOrganizationIfNeeded(Object entity, String operation) {
        try {
            // Buscar o campo 'organization' usando reflection
            Field organizationField = findOrganizationField(entity.getClass());
            
            if (organizationField != null) {
                organizationField.setAccessible(true);
                Object currentValue = organizationField.get(entity);
                
                // Só seta se ainda não foi setado
                if (currentValue == null) {
                    Organization contextOrg = OrganizationContext.getOrganization();
                    
                    if (contextOrg != null) {
                        organizationField.set(entity, contextOrg);
                        log.debug("Organization auto-setada em {} ({}): {} (ID: {})", 
                            entity.getClass().getSimpleName(), operation, 
                            contextOrg.getName(), contextOrg.getId());
                    } else {
                        log.warn("Tentativa de {} entity {} sem Organization no contexto. " +
                                "A operação pode falhar se organization for NOT NULL.", 
                                operation, entity.getClass().getSimpleName());
                    }
                }
            }
        } catch (Exception e) {
            log.error("Erro ao setar organization automaticamente em {}: {}", 
                entity.getClass().getSimpleName(), e.getMessage());
        }
    }

    /**
     * Busca o campo 'organization' na classe ou em suas superclasses
     */
    private Field findOrganizationField(Class<?> clazz) {
        Class<?> currentClass = clazz;
        
        while (currentClass != null && currentClass != Object.class) {
            try {
                return currentClass.getDeclaredField("organization");
            } catch (NoSuchFieldException e) {
                // Tentar na superclasse
                currentClass = currentClass.getSuperclass();
            }
        }
        
        return null;
    }
}

