package com.gomech.controller;

import com.gomech.dto.Tutorial.MarkTutorialViewedDTO;
import com.gomech.dto.Tutorial.TutorialProgressResponseDTO;
import com.gomech.model.User;
import com.gomech.service.TutorialProgressService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users/me/tutorials")
public class TutorialController {

    private final TutorialProgressService tutorialProgressService;

    public TutorialController(TutorialProgressService tutorialProgressService) {
        this.tutorialProgressService = tutorialProgressService;
    }

    @GetMapping
    public ResponseEntity<TutorialProgressResponseDTO> getProgress(
            @AuthenticationPrincipal User user
    ) {
        TutorialProgressResponseDTO progress = tutorialProgressService.getProgress(user);
        return ResponseEntity.ok(progress);
    }

    @PostMapping
    public ResponseEntity<TutorialProgressResponseDTO> markAsViewed(
            @AuthenticationPrincipal User user,
            @RequestBody @Valid MarkTutorialViewedDTO dto
    ) {
        TutorialProgressResponseDTO progress = tutorialProgressService.markAsViewed(user, dto.tutorialKey());
        return ResponseEntity.ok(progress);
    }
}

