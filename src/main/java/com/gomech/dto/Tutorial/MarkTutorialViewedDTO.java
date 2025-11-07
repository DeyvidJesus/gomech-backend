package com.gomech.dto.Tutorial;

import jakarta.validation.constraints.NotBlank;

public record MarkTutorialViewedDTO(
    @NotBlank(message = "Tutorial key é obrigatório")
    String tutorialKey
) {
}

