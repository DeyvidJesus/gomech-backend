package com.gomech.controller;

import com.gomech.dto.Parts.PartCreateDTO;
import com.gomech.dto.Parts.PartResponseDTO;
import com.gomech.dto.Parts.PartUpdateDTO;
import com.gomech.service.PartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/parts")
@Tag(name = "Peças")
@SecurityRequirement(name = "bearer-key")
public class PartController {

    private final PartService partService;

    public PartController(PartService partService) {
        this.partService = partService;
    }

    @Operation(summary = "Cria uma nova peça no catálogo")
    @PostMapping
    public ResponseEntity<PartResponseDTO> create(@Valid @RequestBody PartCreateDTO dto) {
        PartResponseDTO response = partService.register(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Lista todas as peças cadastradas")
    @GetMapping
    public List<PartResponseDTO> list() {
        return partService.listAll();
    }

    @Operation(summary = "Busca uma peça pelo identificador")
    @GetMapping("/{id}")
    public ResponseEntity<PartResponseDTO> get(@PathVariable Long id) {
        return partService.getById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Atualiza os dados de uma peça existente")
    @PutMapping("/{id}")
    public ResponseEntity<PartResponseDTO> update(@PathVariable Long id, @Valid @RequestBody PartUpdateDTO dto) {
        try {
            return ResponseEntity.ok(partService.update(id, dto));
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage(), ex);
        }
    }

    @Operation(summary = "Remove uma peça do catálogo")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        try {
            partService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage(), ex);
        }
    }
}
