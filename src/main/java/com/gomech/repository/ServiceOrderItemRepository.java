package com.gomech.repository;

import com.gomech.model.ServiceOrderItem;
import com.gomech.model.ServiceOrderItemType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServiceOrderItemRepository extends JpaRepository<ServiceOrderItem, Long> {
    
    List<ServiceOrderItem> findByServiceOrderId(Long serviceOrderId);
    
    List<ServiceOrderItem> findByItemType(ServiceOrderItemType itemType);
    
    List<ServiceOrderItem> findByProductCode(String productCode);
    
    List<ServiceOrderItem> findByRequiresStock(Boolean requiresStock);
    
    List<ServiceOrderItem> findByStockReserved(Boolean stockReserved);
    
    List<ServiceOrderItem> findByApplied(Boolean applied);
    
    @Query("SELECT soi FROM ServiceOrderItem soi WHERE soi.serviceOrder.id = :serviceOrderId AND soi.applied = false")
    List<ServiceOrderItem> findPendingItemsByServiceOrder(@Param("serviceOrderId") Long serviceOrderId);
    
    @Query("SELECT soi FROM ServiceOrderItem soi WHERE soi.serviceOrder.id = :serviceOrderId AND soi.applied = true")
    List<ServiceOrderItem> findAppliedItemsByServiceOrder(@Param("serviceOrderId") Long serviceOrderId);
    
    @Query("SELECT soi FROM ServiceOrderItem soi WHERE soi.itemType = 'PART' AND soi.requiresStock = true")
    List<ServiceOrderItem> findPartsRequiringStock();
    
    @Query("SELECT soi FROM ServiceOrderItem soi WHERE soi.stockReserved = true AND soi.applied = false")
    List<ServiceOrderItem> findReservedNotAppliedItems();
    
    @Query("SELECT soi FROM ServiceOrderItem soi WHERE soi.serviceOrder.id = :serviceOrderId AND soi.itemType = :itemType")
    List<ServiceOrderItem> findByServiceOrderAndType(@Param("serviceOrderId") Long serviceOrderId, 
                                                     @Param("itemType") ServiceOrderItemType itemType);
    
    @Query("SELECT soi.description, COUNT(soi) as quantidade FROM ServiceOrderItem soi GROUP BY soi.description ORDER BY quantidade DESC")
    List<Object[]> findMostUsedItems();
    
    @Query("SELECT soi FROM ServiceOrderItem soi WHERE soi.productCode = :productCode AND soi.stockReserved = true")
    List<ServiceOrderItem> findReservedByProductCode(@Param("productCode") String productCode);
}
