package com.gomech.repository;

import com.gomech.domain.InventoryItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryItemRepository extends JpaRepository<InventoryItem, Long> {
    List<InventoryItem> findByPartId(Long partId);

    Optional<InventoryItem> findByPartIdAndLocation(Long partId, String location);
}
