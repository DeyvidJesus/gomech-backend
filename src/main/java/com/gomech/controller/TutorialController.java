package com.gomech.controller;

import com.gomech.dto.Tutorial.MarkTutorialViewedDTO;
import com.gomech.dto.Tutorial.TutorialProgressResponseDTO;
import com.gomech.model.User;
import com.gomech.service.TutorialProgressService;
import com.gomech.util.OrganizationContext;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users/me/tutorials")
public class TutorialController {

    private static final Logger logger = LoggerFactory.getLogger(TutorialController.class);
    
    private final TutorialProgressService tutorialProgressService;
    private final OrganizationContext organizationContext;

    public TutorialController(TutorialProgressService tutorialProgressService, OrganizationContext organizationContext) {
        this.tutorialProgressService = tutorialProgressService;
        this.organizationContext = organizationContext;
    }

    @GetMapping
    public ResponseEntity<TutorialProgressResponseDTO> getProgress(
            @AuthenticationPrincipal User user
    ) {
        // Exemplo de uso do OrganizationContext
        organizationContext.getCurrentOrganizationId().ifPresent(orgId -> 
            logger.info("Requisição de tutorial do usuário {} da organização {}", user.getId(), orgId)
        );
        
        TutorialProgressResponseDTO progress = tutorialProgressService.getProgress(user);
        return ResponseEntity.ok(progress);
    }

    @PostMapping
    public ResponseEntity<TutorialProgressResponseDTO> markAsViewed(
            @AuthenticationPrincipal User user,
            @RequestBody @Valid MarkTutorialViewedDTO dto
    ) {
        // Exemplo de uso do OrganizationContext
        organizationContext.getCurrentOrganizationId().ifPresent(orgId -> 
            logger.info("Marcando tutorial '{}' como visto para usuário {} da organização {}", 
                dto.tutorialKey(), user.getId(), orgId)
        );
        
        TutorialProgressResponseDTO progress = tutorialProgressService.markAsViewed(user, dto.tutorialKey());
        return ResponseEntity.ok(progress);
    }
}

