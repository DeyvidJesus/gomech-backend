package com.gomech.service;

import com.gomech.domain.Part;
import com.gomech.dto.Parts.PartCreateDTO;
import com.gomech.dto.Parts.PartMapper;
import com.gomech.dto.Parts.PartResponseDTO;
import com.gomech.dto.Parts.PartUpdateDTO;
import com.gomech.repository.PartRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class PartService {

    private final PartRepository partRepository;
    private final AuditService auditService;

    public PartService(PartRepository partRepository, AuditService auditService) {
        this.partRepository = partRepository;
        this.auditService = auditService;
    }

    public PartResponseDTO register(PartCreateDTO dto) {
        Part part = PartMapper.toEntity(dto);
        Part saved = partRepository.save(part);
        auditService.logEntityAction("CREATE", "PART", saved.getId(),
                "Peça cadastrada: " + saved.getName());
        return PartResponseDTO.fromEntity(saved);
    }

    public PartResponseDTO update(Long id, PartUpdateDTO updates) {
        Part existing = partRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Peça não encontrada"));

        PartMapper.updateEntity(existing, updates);

        Part saved = partRepository.save(existing);
        auditService.logEntityAction("UPDATE", "PART", saved.getId(),
                "Peça atualizada: " + saved.getName());
        return PartResponseDTO.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public Optional<PartResponseDTO> getById(Long id) {
        return partRepository.findById(id)
                .map(PartResponseDTO::fromEntity);
    }

    @Transactional(readOnly = true)
    public List<PartResponseDTO> listAll() {
        return partRepository.findAll().stream()
                .map(PartResponseDTO::fromEntity)
                .toList();
    }

    public void delete(Long id) {
        if (!partRepository.existsById(id)) {
            throw new IllegalArgumentException("Peça não encontrada");
        }
        partRepository.deleteById(id);
        auditService.logEntityAction("DELETE", "PART", id,
                "Peça removida");
    }
}
