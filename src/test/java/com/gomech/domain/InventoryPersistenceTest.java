package com.gomech.domain;

import com.gomech.model.Client;
import com.gomech.model.Organization;
import com.gomech.model.ServiceOrder;
import com.gomech.model.Vehicle;
import com.gomech.repository.ClientRepository;
import com.gomech.repository.InventoryItemRepository;
import com.gomech.repository.InventoryMovementRepository;
import com.gomech.repository.PartRepository;
import com.gomech.repository.ServiceOrderRepository;
import com.gomech.repository.VehicleRepository;
import com.gomech.repository.OrganizationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@TestPropertySource(properties = "spring.flyway.enabled=false")
class InventoryPersistenceTest {

    @Autowired
    private PartRepository partRepository;

    @Autowired
    private InventoryItemRepository inventoryItemRepository;

    @Autowired
    private InventoryMovementRepository inventoryMovementRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private ServiceOrderRepository serviceOrderRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Test
    void shouldPersistInventoryMovementWithRelationships() {
        Client client = new Client();
        client.setName("John Doe");
        client.setEmail("john@example.com");
        Organization organization = organizationRepository.findById(1L).orElseThrow();
        client.setOrganization(organization);
        client = clientRepository.save(client);

        Vehicle vehicle = new Vehicle();
        vehicle.setClient(client);
        vehicle.setLicensePlate("ABC1D23");
        vehicle.setBrand("Ford");
        vehicle.setModel("Focus");
        vehicle.setManufactureDate(new Date());
        vehicle.setColor("Blue");
        vehicle.setObservations("Test vehicle");
        vehicle.setKilometers(12000);
        vehicle.setChassisId("CHS123456789");
        vehicle.setOrganization(organization);
        vehicle = vehicleRepository.save(vehicle);

        ServiceOrder serviceOrder = new ServiceOrder();
        serviceOrder.setClient(client);
        serviceOrder.setVehicle(vehicle);
        serviceOrder.setDescription("Brake replacement");
        serviceOrder.setOrganization(organization);
        serviceOrder = serviceOrderRepository.save(serviceOrder);

        Part part = new Part();
        part.setName("Brake Pad");
        part.setSku("BRK-001");
        part.setManufacturer("ACME");
        part.setUnitCost(new BigDecimal("50.00"));
        part.setUnitPrice(new BigDecimal("80.00"));
        part.setOrganization(organization);
        part = partRepository.save(part);

        InventoryItem item = new InventoryItem();
        item.setPart(part);
        item.setLocation("MAIN");
        item.setQuantity(10);
        item.setReservedQuantity(1);
        item.setMinimumQuantity(2);
        item.setUnitCost(new BigDecimal("50.00"));
        item.setSalePrice(new BigDecimal("80.00"));
        item.setOrganization(organization);
        item = inventoryItemRepository.save(item);

        InventoryMovement movement = new InventoryMovement();
        movement.setInventoryItem(item);
        movement.setPart(part);
        movement.setVehicle(vehicle);
        movement.setMovementType(InventoryMovementType.OUT);
        movement.setQuantity(2);
        movement.setReferenceCode("SO-" + serviceOrder.getId());
        movement.setNotes("Used for brake replacement");
        movement.setOrganization(organization);
        serviceOrder.addInventoryMovement(movement);
        serviceOrderRepository.saveAndFlush(serviceOrder);

        entityManager.flush();
        entityManager.clear();

        List<InventoryMovement> serviceOrderMovements = inventoryMovementRepository.findByServiceOrderId(serviceOrder.getId());
        assertThat(serviceOrderMovements)
                .hasSize(1)
                .first()
                .extracting(mov -> mov.getVehicle().getId())
                .isEqualTo(vehicle.getId());

        List<InventoryMovement> vehicleMovements = inventoryMovementRepository.findByVehicleId(vehicle.getId());
        assertThat(vehicleMovements).hasSize(1);

        ServiceOrder reloadedOrder = serviceOrderRepository.findById(serviceOrder.getId()).orElseThrow();
        assertThat(reloadedOrder.getInventoryMovements()).hasSize(1);

        InventoryItem reloadedItem = inventoryItemRepository.findById(item.getId()).orElseThrow();
        assertThat(reloadedItem.getMovements()).hasSize(1);

        assertThat(partRepository.findBySku("BRK-001")).isPresent();
    }
}
