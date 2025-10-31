package com.gomech.service;

import com.gomech.domain.InventoryItem;
import com.gomech.domain.InventoryMovement;
import com.gomech.domain.InventoryMovementType;
import com.gomech.domain.Part;
import com.gomech.model.Client;
import com.gomech.model.ServiceOrder;
import com.gomech.model.ServiceOrderItem;
import com.gomech.model.ServiceOrderItemType;
import com.gomech.model.Vehicle;
import com.gomech.repository.InventoryItemRepository;
import com.gomech.repository.InventoryMovementRepository;
import com.gomech.repository.PartRepository;
import com.gomech.repository.ServiceOrderItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private InventoryItemRepository inventoryItemRepository;

    @Mock
    private InventoryMovementRepository inventoryMovementRepository;

    @Mock
    private PartRepository partRepository;

    @Mock
    private ServiceOrderItemRepository serviceOrderItemRepository;

    @Mock
    private InventoryAlertService inventoryAlertService;

    @InjectMocks
    private InventoryService inventoryService;

    private Part part;
    private InventoryItem inventoryItem;
    private ServiceOrder serviceOrder;
    private ServiceOrderItem serviceOrderItem;

    @BeforeEach
    void init() {
        part = new Part();
        part.setId(1L);
        part.setName("Filtro de óleo");
        part.setSku("FLT-001");
        part.setUnitCost(new BigDecimal("30.00"));
        part.setUnitPrice(new BigDecimal("50.00"));

        inventoryItem = new InventoryItem();
        inventoryItem.setId(10L);
        inventoryItem.setPart(part);
        inventoryItem.setLocation("MAIN");
        inventoryItem.setQuantity(10);
        inventoryItem.setReservedQuantity(0);

        Client client = new Client();
        client.setId(100L);
        client.setName("Jane");
        Vehicle vehicle = new Vehicle();
        vehicle.setId(200L);
        vehicle.setClient(client);
        vehicle.setLicensePlate("XYZ9Z99");

        serviceOrder = new ServiceOrder();
        serviceOrder.setId(300L);
        serviceOrder.setClient(client);
        serviceOrder.setVehicle(vehicle);
        serviceOrder.setOrderNumber("OS-TEST");

        serviceOrderItem = new ServiceOrderItem();
        serviceOrderItem.setId(400L);
        serviceOrderItem.setServiceOrder(serviceOrder);
        serviceOrderItem.setDescription("Filtro de óleo");
        serviceOrderItem.setItemType(ServiceOrderItemType.PART);
        serviceOrderItem.setQuantity(2);
        serviceOrderItem.setUnitPrice(new BigDecimal("55.00"));
        serviceOrderItem.setRequiresStock(true);
        serviceOrderItem.setStockProductId(inventoryItem.getId());

        when(inventoryItemRepository.findById(anyLong())).thenReturn(Optional.of(inventoryItem));
        when(inventoryItemRepository.save(any(InventoryItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(serviceOrderItemRepository.save(any(ServiceOrderItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(inventoryMovementRepository.save(any(InventoryMovement.class))).thenAnswer(invocation -> {
            InventoryMovement movement = invocation.getArgument(0);
            movement.setId(500L);
            movement.setMovementDate(LocalDateTime.now());
            return movement;
        });
        doNothing().when(inventoryAlertService).onStockLevelChanged(any(InventoryItem.class));
    }

    @Test
    void shouldReserveStockForServiceOrderItem() {
        InventoryMovement movement = inventoryService.reserveStock(serviceOrder, serviceOrderItem,
                serviceOrderItem.getQuantity(), "Reserva de teste");

        assertThat(inventoryItem.getReservedQuantity()).isEqualTo(2);
        assertThat(serviceOrderItem.getStockReserved()).isTrue();
        assertThat(movement.getMovementType()).isEqualTo(InventoryMovementType.ADJUSTMENT);
        assertThat(movement.getServiceOrder()).isEqualTo(serviceOrder);

        ArgumentCaptor<InventoryMovement> movementCaptor = ArgumentCaptor.forClass(InventoryMovement.class);
        verify(inventoryMovementRepository).save(movementCaptor.capture());
        assertThat(movementCaptor.getValue().getMovementType()).isEqualTo(InventoryMovementType.ADJUSTMENT);
    }

    @Test
    void shouldConsumeStockWhenItemApplied() {
        inventoryItem.setReservedQuantity(2);
        serviceOrderItem.setStockReserved(true);

        InventoryMovement movement = inventoryService.consumeStock(serviceOrder, serviceOrderItem,
                serviceOrderItem.getQuantity(), "Baixa de teste");

        assertThat(inventoryItem.getQuantity()).isEqualTo(8);
        assertThat(inventoryItem.getReservedQuantity()).isZero();
        assertThat(serviceOrderItem.getStockReserved()).isFalse();
        assertThat(movement.getMovementType()).isEqualTo(InventoryMovementType.OUT);
    }

    @Test
    void shouldCancelReservationWhenItemReleased() {
        inventoryItem.setReservedQuantity(3);
        serviceOrderItem.setStockReserved(true);
        serviceOrderItem.setQuantity(3);

        InventoryMovement movement = inventoryService.cancelReservation(serviceOrder, serviceOrderItem,
                serviceOrderItem.getQuantity(), "Cancelamento de teste");

        assertThat(inventoryItem.getReservedQuantity()).isZero();
        assertThat(serviceOrderItem.getStockReserved()).isFalse();
        assertThat(movement.getMovementType()).isEqualTo(InventoryMovementType.ADJUSTMENT);
    }

    @Test
    void shouldReconcileInventoryWhenOrderCancelled() {
        ServiceOrderItem reservedItem = cloneItemWithQuantity(2L, 2, false, true);
        ServiceOrderItem appliedItem = cloneItemWithQuantity(3L, 1, true, false);

        inventoryItem.setReservedQuantity(reservedItem.getQuantity());
        inventoryItem.setQuantity(7);

        ServiceOrder order = serviceOrder;
        order.getItems().clear();
        order.addItem(reservedItem);
        order.addItem(appliedItem);

        inventoryService.reconcileServiceOrderInventory(order);

        verify(inventoryItemRepository, times(4)).save(any(InventoryItem.class));
        verify(inventoryMovementRepository, times(2)).save(any(InventoryMovement.class));

        assertThat(reservedItem.getStockReserved()).isFalse();
        assertThat(appliedItem.getApplied()).isFalse();
        assertThat(inventoryItem.getReservedQuantity()).isZero();
        assertThat(inventoryItem.getQuantity()).isEqualTo(8);
    }

    private ServiceOrderItem cloneItemWithQuantity(Long id, int quantity, boolean applied, boolean reservedOnly) {
        ServiceOrderItem item = new ServiceOrderItem();
        item.setId(id);
        item.setServiceOrder(serviceOrder);
        item.setDescription("Peça " + id);
        item.setItemType(ServiceOrderItemType.PART);
        item.setQuantity(quantity);
        item.setUnitPrice(new BigDecimal("42.00"));
        item.setRequiresStock(true);
        item.setStockProductId(inventoryItem.getId());
        if (applied) {
            item.apply();
        }
        item.setStockReserved(reservedOnly);
        when(serviceOrderItemRepository.save(item)).thenReturn(item);
        when(inventoryItemRepository.findById(item.getStockProductId())).thenReturn(Optional.of(inventoryItem));
        return item;
    }
}
