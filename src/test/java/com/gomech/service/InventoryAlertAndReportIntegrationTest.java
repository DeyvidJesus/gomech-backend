package com.gomech.service;

import com.gomech.domain.InventoryItem;
import com.gomech.domain.Part;
import com.gomech.dto.Inventory.CriticalPartReportDTO;
import com.gomech.dto.Inventory.PartAvailabilityDTO;
import com.gomech.dto.Inventory.PartConsumptionStats;
import com.gomech.dto.Inventory.StockConsumptionRequestDTO;
import com.gomech.dto.Inventory.StockReservationRequestDTO;
import com.gomech.model.Client;
import com.gomech.model.ServiceOrder;
import com.gomech.model.ServiceOrderItem;
import com.gomech.model.ServiceOrderItemType;
import com.gomech.model.Vehicle;
import com.gomech.notification.NotificationGateway;
import com.gomech.notification.NotificationPayload;
import com.gomech.repository.InventoryItemRepository;
import com.gomech.repository.PartRepository;
import com.gomech.repository.ServiceOrderItemRepository;
import com.gomech.repository.ServiceOrderRepository;
import com.gomech.repository.VehicleRepository;
import com.gomech.repository.ClientRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

@SpringBootTest
@Transactional
class InventoryAlertAndReportIntegrationTest {

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private InventoryReportService inventoryReportService;

    @Autowired
    private PartRepository partRepository;

    @Autowired
    private InventoryItemRepository inventoryItemRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private ServiceOrderRepository serviceOrderRepository;

    @Autowired
    private ServiceOrderItemRepository serviceOrderItemRepository;

    @MockBean
    private NotificationGateway notificationGateway;

    @Test
    void shouldNotifyAndUpdateReportsWhenStockBecomesCritical() {
        Part part = new Part();
        part.setName("Filtro de óleo");
        part.setSku("FLT-001");
        part.setUnitCost(new BigDecimal("35.00"));
        part.setUnitPrice(new BigDecimal("60.00"));
        Part savedPart = partRepository.save(part);

        InventoryItem inventoryItem = new InventoryItem();
        inventoryItem.setPart(savedPart);
        inventoryItem.setLocation("MAIN");
        inventoryItem.setQuantity(5);
        inventoryItem.setReservedQuantity(0);
        inventoryItem.setMinimumQuantity(2);
        InventoryItem savedInventoryItem = inventoryItemRepository.save(inventoryItem);

        Client client = new Client();
        client.setName("João Mecânico");
        Client savedClient = clientRepository.save(client);

        Vehicle vehicle = new Vehicle();
        vehicle.setClient(savedClient);
        vehicle.setLicensePlate("ABC1D23");
        vehicle.setBrand("Ford");
        vehicle.setModel("Sedan");
        vehicle.setManufactureDate(new Date());
        vehicle.setColor("Preto");
        vehicle.setKilometers(120000);
        vehicle.setChassisId("CHASSIS123");
        Vehicle savedVehicle = vehicleRepository.save(vehicle);

        ServiceOrder serviceOrder = new ServiceOrder();
        serviceOrder.setClient(savedClient);
        serviceOrder.setVehicle(savedVehicle);
        serviceOrder.setDescription("Revisão completa");
        ServiceOrder savedServiceOrder = serviceOrderRepository.save(serviceOrder);

        ServiceOrderItem serviceOrderItem = new ServiceOrderItem();
        serviceOrderItem.setServiceOrder(savedServiceOrder);
        serviceOrderItem.setDescription("Filtro de óleo");
        serviceOrderItem.setItemType(ServiceOrderItemType.PART);
        serviceOrderItem.setQuantity(4);
        serviceOrderItem.setUnitPrice(new BigDecimal("65.00"));
        serviceOrderItem.setRequiresStock(true);
        ServiceOrderItem savedServiceOrderItem = serviceOrderItemRepository.save(serviceOrderItem);

        inventoryService.reserveStock(new StockReservationRequestDTO(savedServiceOrderItem.getId(), 4, "Reserva automática"));
        inventoryService.consumeStock(new StockConsumptionRequestDTO(savedServiceOrderItem.getId(), 4, "Consumo para teste"));

        ArgumentCaptor<NotificationPayload> payloadCaptor = ArgumentCaptor.forClass(NotificationPayload.class);
        verify(notificationGateway, atLeastOnce()).sendLowStockAlert(payloadCaptor.capture());
        NotificationPayload lastPayload = payloadCaptor.getValue();
        assertThat(lastPayload.partId()).isEqualTo(savedPart.getId());
        assertThat(lastPayload.availableQuantity()).isLessThanOrEqualTo(savedInventoryItem.getMinimumQuantity());

        List<InventoryItem> persistedItems = inventoryItemRepository.findAll();
        assertThat(persistedItems).hasSize(1);
        InventoryItem persistedItem = persistedItems.getFirst();
        assertThat(persistedItem.getQuantity()).isEqualTo(1);
        assertThat(persistedItem.getMinimumQuantity()).isEqualTo(2);

        PartAvailabilityDTO availability = inventoryReportService.getAvailabilityForPart(savedPart.getId());
        assertThat(availability.availableQuantity()).isEqualTo(1L);
        assertThat(availability.minimumQuantity()).isEqualTo(2L);

        List<CriticalPartReportDTO> criticalParts = inventoryReportService.listCriticalParts(savedVehicle.getModel());
        assertThat(criticalParts)
                .anySatisfy(report -> {
                    assertThat(report.partId()).isEqualTo(savedPart.getId());
                    assertThat(report.availableQuantity()).isEqualTo(1L);
                    assertThat(report.vehicleModel()).isEqualToIgnoringCase(savedVehicle.getModel());
                });

        List<PartAvailabilityDTO> vehicleAvailability = inventoryReportService.listAvailabilityForVehicle(savedVehicle.getId());
        assertThat(vehicleAvailability)
                .anySatisfy(entry -> assertThat(entry.availableQuantity()).isEqualTo(1L));

        List<PartAvailabilityDTO> clientAvailability = inventoryReportService.listAvailabilityForClient(savedClient.getId());
        assertThat(clientAvailability)
                .anySatisfy(entry -> assertThat(entry.partId()).isEqualTo(savedPart.getId()));

        List<PartConsumptionStats> vehicleHistory = inventoryReportService.getVehicleConsumptionHistory(savedVehicle.getId());
        assertThat(vehicleHistory)
                .anySatisfy(stats -> {
                    assertThat(stats.partId()).isEqualTo(savedPart.getId());
                    assertThat(stats.totalQuantity()).isEqualTo(4L);
                });

        List<PartConsumptionStats> clientHistory = inventoryReportService.getClientConsumptionHistory(savedClient.getId());
        assertThat(clientHistory)
                .anySatisfy(stats -> {
                    assertThat(stats.partId()).isEqualTo(savedPart.getId());
                    assertThat(stats.distinctVehicles()).isEqualTo(1L);
                });
    }
}
