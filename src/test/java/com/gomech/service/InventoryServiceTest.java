package com.gomech.service;

import com.gomech.model.InventoryMovement;
import com.gomech.model.InventoryMovementType;
import com.gomech.model.Part;
import com.gomech.model.ServiceOrderItem;
import com.gomech.repository.InventoryMovementRepository;
import com.gomech.repository.PartRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class InventoryServiceTest {

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private PartService partService;

    @Autowired
    private PartRepository partRepository;

    @Autowired
    private InventoryMovementRepository movementRepository;

    private Part part;
    private ServiceOrderItem serviceOrderItem;

    @BeforeEach
    void setUp() {
        Part newPart = new Part();
        newPart.setCode("P-001");
        newPart.setName("Filtro de óleo");
        newPart.setQuantity(10);
        part = partService.registerPart(newPart);

        serviceOrderItem = new ServiceOrderItem();
        serviceOrderItem.setId(1L);
        serviceOrderItem.setDescription("Uso teste");
    }

    @Test
    void shouldReserveStockAndRegisterMovement() {
        inventoryService.reservePart(part.getId(), 3, serviceOrderItem, "Reserva teste");

        Part updated = partRepository.findById(part.getId()).orElseThrow();
        assertEquals(10, updated.getQuantity());
        assertEquals(3, updated.getReservedQuantity());
        assertEquals(7, updated.getAvailableQuantity());

        InventoryMovement last = getLastMovement();
        assertEquals(InventoryMovementType.ADJUST, last.getType());
        assertEquals(3, last.getReservedChange());
        assertEquals("Reserva teste", last.getDescription());
    }

    @Test
    void shouldConsumeReservedStockAndRegisterOutMovement() {
        inventoryService.reservePart(part.getId(), 4, serviceOrderItem, "Reserva para consumo");
        inventoryService.consumeReservedPart(part.getId(), 4, serviceOrderItem, "Baixa da peça");

        Part updated = partRepository.findById(part.getId()).orElseThrow();
        assertEquals(6, updated.getQuantity());
        assertEquals(0, updated.getReservedQuantity());

        InventoryMovement last = getLastMovement();
        assertEquals(InventoryMovementType.OUT, last.getType());
        assertEquals(-4, last.getReservedChange());
        assertEquals("Baixa da peça", last.getDescription());
    }

    @Test
    void shouldReleaseReservationWhenCancelled() {
        inventoryService.reservePart(part.getId(), 2, serviceOrderItem, "Reserva inicial");
        inventoryService.releaseReservation(part.getId(), 2, serviceOrderItem, "Cancelamento da reserva");

        Part updated = partRepository.findById(part.getId()).orElseThrow();
        assertEquals(10, updated.getQuantity());
        assertEquals(0, updated.getReservedQuantity());

        InventoryMovement last = getLastMovement();
        assertEquals(InventoryMovementType.ADJUST, last.getType());
        assertEquals(-2, last.getReservedChange());
        assertEquals("Cancelamento da reserva", last.getDescription());
    }

    @Test
    void shouldReconcileStockWithAdjustmentMovement() {
        partService.reconcileStock(part.getId(), 12, "Conciliação manual");

        Part updated = partRepository.findById(part.getId()).orElseThrow();
        assertEquals(12, updated.getQuantity());
        assertEquals(0, updated.getReservedQuantity());

        InventoryMovement last = getLastMovement();
        assertEquals(InventoryMovementType.ADJUST, last.getType());
        assertEquals("Conciliação manual", last.getDescription());
        assertEquals(0, last.getReservedChange());
    }

    private InventoryMovement getLastMovement() {
        List<InventoryMovement> movements = movementRepository.findByPartIdOrderByCreatedAtAsc(part.getId());
        assertFalse(movements.isEmpty());
        return movements.get(movements.size() - 1);
    }
}
