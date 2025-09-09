package com.gomech.repository;

import com.gomech.model.ServiceOrder;
import com.gomech.model.ServiceOrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ServiceOrderRepository extends JpaRepository<ServiceOrder, Long> {
    Optional<ServiceOrder> findByOrderNumber(String orderNumber);
    List<ServiceOrder> findByStatus(ServiceOrderStatus status);
    List<ServiceOrder> findByClientId(Long clientId);
    List<ServiceOrder> findByVehicleId(Long vehicleId);

    @Query("SELECT so FROM ServiceOrder so WHERE so.estimatedCompletion < :date AND so.status NOT IN ('COMPLETED', 'CANCELLED', 'DELIVERED')")
    List<ServiceOrder> findOverdueOrders(@Param("date") LocalDateTime date);

    @Query("SELECT so FROM ServiceOrder so WHERE so.status = 'WAITING_PARTS'")
    List<ServiceOrder> findWaitingParts();

    @Query("SELECT so FROM ServiceOrder so WHERE so.status = 'WAITING_APPROVAL'")
    List<ServiceOrder> findWaitingApproval();

    @Query("SELECT so FROM ServiceOrder so WHERE so.vehicle.id = :vehicleId ORDER BY so.createdAt DESC")
    List<ServiceOrder> findVehicleHistory(@Param("vehicleId") Long vehicleId);
}
