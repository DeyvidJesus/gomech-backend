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
    
    List<ServiceOrder> findByTechnicianName(String technicianName);
    
    @Query("SELECT so FROM ServiceOrder so WHERE so.createdAt BETWEEN :startDate AND :endDate")
    List<ServiceOrder> findByDateRange(@Param("startDate") LocalDateTime startDate, 
                                      @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT so FROM ServiceOrder so WHERE so.status IN :statuses")
    List<ServiceOrder> findByStatusIn(@Param("statuses") List<ServiceOrderStatus> statuses);
    
    @Query("SELECT so FROM ServiceOrder so WHERE so.estimatedCompletion < :date AND so.status NOT IN ('COMPLETED', 'CANCELLED', 'DELIVERED')")
    List<ServiceOrder> findOverdueOrders(@Param("date") LocalDateTime date);
    
    @Query("SELECT so FROM ServiceOrder so WHERE so.status = 'WAITING_PARTS'")
    List<ServiceOrder> findWaitingParts();
    
    @Query("SELECT so FROM ServiceOrder so WHERE so.status = 'WAITING_APPROVAL'")
    List<ServiceOrder> findWaitingApproval();
    
    // Query para buscar por cliente e período
    @Query("SELECT so FROM ServiceOrder so WHERE so.clientId = :clientId AND so.createdAt BETWEEN :startDate AND :endDate")
    List<ServiceOrder> findByClientAndDateRange(@Param("clientId") Long clientId,
                                               @Param("startDate") LocalDateTime startDate,
                                               @Param("endDate") LocalDateTime endDate);
    
    // Query para buscar por veículo e período
    @Query("SELECT so FROM ServiceOrder so WHERE so.vehicleId = :vehicleId AND so.createdAt BETWEEN :startDate AND :endDate")
    List<ServiceOrder> findByVehicleAndDateRange(@Param("vehicleId") Long vehicleId,
                                                 @Param("startDate") LocalDateTime startDate,
                                                 @Param("endDate") LocalDateTime endDate);
    
    // Contar ordens por status
    long countByStatus(ServiceOrderStatus status);
    
    // Buscar últimas ordens de um cliente
    @Query("SELECT so FROM ServiceOrder so WHERE so.clientId = :clientId ORDER BY so.createdAt DESC")
    List<ServiceOrder> findLastOrdersByClient(@Param("clientId") Long clientId);
    
    // Buscar histórico de um veículo
    @Query("SELECT so FROM ServiceOrder so WHERE so.vehicleId = :vehicleId ORDER BY so.createdAt DESC")
    List<ServiceOrder> findVehicleHistory(@Param("vehicleId") Long vehicleId);
}
