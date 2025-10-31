package com.gomech.service;

import com.gomech.domain.Part;
import com.gomech.repository.PartRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class PartService {

    private final PartRepository partRepository;

    public PartService(PartRepository partRepository) {
        this.partRepository = partRepository;
    }

    public Part register(Part part) {
        return partRepository.save(part);
    }

    public Part update(Long id, Part updates) {
        Part existing = partRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Peça não encontrada"));

        if (updates.getName() != null) existing.setName(updates.getName());
        if (updates.getSku() != null) existing.setSku(updates.getSku());
        if (updates.getManufacturer() != null) existing.setManufacturer(updates.getManufacturer());
        if (updates.getDescription() != null) existing.setDescription(updates.getDescription());
        if (updates.getUnitCost() != null) existing.setUnitCost(updates.getUnitCost());
        if (updates.getUnitPrice() != null) existing.setUnitPrice(updates.getUnitPrice());
        if (updates.getActive() != null) existing.setActive(updates.getActive());

        return partRepository.save(existing);
    }

    @Transactional(readOnly = true)
    public Part getById(Long id) {
        return partRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Peça não encontrada"));
    }

    @Transactional(readOnly = true)
    public List<Part> listAll() {
        return partRepository.findAll();
    }
}
