package com.gomech.service;

import com.gomech.dto.*;
import com.gomech.model.*;
import com.gomech.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class ServiceOrderService {
    
    @Autowired
    private ServiceOrderRepository serviceOrderRepository;
    
    @Autowired
    private ServiceOrderItemRepository serviceOrderItemRepository;
    
    @Autowired
    private VehicleRepository vehicleRepository;
    
    @Autowired
    private ClientRepository clientRepository;

    public ServiceOrderResponseDTO create(ServiceOrderCreateDTO dto) {
        // Validar se veículo existe
        Vehicle vehicle = vehicleRepository.findById(dto.getVehicleId())
            .orElseThrow(() -> new RuntimeException("Veículo não encontrado"));
        
        // Validar se cliente existe
        Client client = clientRepository.findById(dto.getClientId())
            .orElseThrow(() -> new RuntimeException("Cliente não encontrado"));

        // Criar ordem de serviço
        ServiceOrder serviceOrder = new ServiceOrder();
        serviceOrder.setVehicle(vehicle);
        serviceOrder.setClient(client);
        serviceOrder.setDescription(dto.getDescription());
        serviceOrder.setProblemDescription(dto.getProblemDescription());
        serviceOrder.setTechnicianName(dto.getTechnicianName());
        serviceOrder.setCurrentKilometers(dto.getCurrentKilometers());
        serviceOrder.setEstimatedCompletion(dto.getEstimatedCompletion());
        serviceOrder.setObservations(dto.getObservations());

        // Criar itens se fornecidos
        if (dto.getItems() != null && !dto.getItems().isEmpty()) {
            for (ServiceOrderItemCreateDTO itemDto : dto.getItems()) {
                ServiceOrderItem item = new ServiceOrderItem();
                item.setDescription(itemDto.getDescription());
                item.setItemType(itemDto.getItemType());
                item.setQuantity(itemDto.getQuantity());
                item.setUnitPrice(itemDto.getUnitPrice());
                item.setProductCode(itemDto.getProductCode());
                item.setRequiresStock(itemDto.getRequiresStock());
                item.setObservations(itemDto.getObservations());
                
                serviceOrder.addItem(item);
            }
        }

        ServiceOrder saved = serviceOrderRepository.save(serviceOrder);
        return convertToResponseDTO(saved);
    }

    public List<ServiceOrderResponseDTO> listAll() {
        return serviceOrderRepository.findAll().stream()
                .map(this::convertToResponseDTO)
                .collect(Collectors.toList());
    }

    public Optional<ServiceOrderResponseDTO> getById(Long id) {
        return serviceOrderRepository.findById(id)
                .map(this::convertToResponseDTO);
    }

    public Optional<ServiceOrderResponseDTO> getByOrderNumber(String orderNumber) {
        return serviceOrderRepository.findByOrderNumber(orderNumber)
                .map(this::convertToResponseDTO);
    }

    public List<ServiceOrderResponseDTO> getByStatus(ServiceOrderStatus status) {
        return serviceOrderRepository.findByStatus(status).stream()
                .map(this::convertToResponseDTO)
                .collect(Collectors.toList());
    }

    public List<ServiceOrderResponseDTO> getByClientId(Long clientId) {
        return serviceOrderRepository.findByClientId(clientId).stream()
                .map(this::convertToResponseDTO)
                .collect(Collectors.toList());
    }

    public List<ServiceOrderResponseDTO> getByVehicleId(Long vehicleId) {
        return serviceOrderRepository.findByVehicleId(vehicleId).stream()
                .map(this::convertToResponseDTO)
                .collect(Collectors.toList());
    }

    public ServiceOrderResponseDTO update(Long id, ServiceOrderUpdateDTO dto) {
        ServiceOrder serviceOrder = serviceOrderRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Ordem de serviço não encontrada"));
        
        // Atualizar campos
        if (dto.getDescription() != null) {
            serviceOrder.setDescription(dto.getDescription());
        }
        if (dto.getProblemDescription() != null) {
            serviceOrder.setProblemDescription(dto.getProblemDescription());
        }
        if (dto.getDiagnosis() != null) {
            serviceOrder.setDiagnosis(dto.getDiagnosis());
        }
        if (dto.getSolutionDescription() != null) {
            serviceOrder.setSolutionDescription(dto.getSolutionDescription());
        }
        if (dto.getStatus() != null) {
            serviceOrder.setStatus(dto.getStatus());
            
            // Se foi concluída, registrar data de conclusão
            if (dto.getStatus() == ServiceOrderStatus.COMPLETED) {
                serviceOrder.setActualCompletion(LocalDateTime.now());
            }
        }
        if (dto.getLaborCost() != null) {
            serviceOrder.setLaborCost(dto.getLaborCost());
        }
        if (dto.getPartsCost() != null) {
            serviceOrder.setPartsCost(dto.getPartsCost());
        }
        if (dto.getDiscount() != null) {
            serviceOrder.setDiscount(dto.getDiscount());
        }
        if (dto.getEstimatedCompletion() != null) {
            serviceOrder.setEstimatedCompletion(dto.getEstimatedCompletion());
        }
        if (dto.getObservations() != null) {
            serviceOrder.setObservations(dto.getObservations());
        }
        if (dto.getTechnicianName() != null) {
            serviceOrder.setTechnicianName(dto.getTechnicianName());
        }
        if (dto.getCurrentKilometers() != null) {
            serviceOrder.setCurrentKilometers(dto.getCurrentKilometers());
        }

        serviceOrder.calculateTotalCost();
        ServiceOrder updated = serviceOrderRepository.save(serviceOrder);
        return convertToResponseDTO(updated);
    }

    public ServiceOrderResponseDTO updateStatus(Long id, ServiceOrderStatus newStatus) {
        ServiceOrder serviceOrder = serviceOrderRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Ordem de serviço não encontrada"));
        
        serviceOrder.setStatus(newStatus);
        
        // Se foi concluída, registrar data de conclusão
        if (newStatus == ServiceOrderStatus.COMPLETED) {
            serviceOrder.setActualCompletion(LocalDateTime.now());
        }
        
        ServiceOrder updated = serviceOrderRepository.save(serviceOrder);
        return convertToResponseDTO(updated);
    }

    public void delete(Long id) {
        if (!serviceOrderRepository.existsById(id)) {
            throw new RuntimeException("Ordem de serviço não encontrada");
        }
        serviceOrderRepository.deleteById(id);
    }

    // Métodos de relatório
    public List<ServiceOrderResponseDTO> getOverdueOrders() {
        return serviceOrderRepository.findOverdueOrders(LocalDateTime.now()).stream()
                .map(this::convertToResponseDTO)
                .collect(Collectors.toList());
    }

    public List<ServiceOrderResponseDTO> getWaitingParts() {
        return serviceOrderRepository.findWaitingParts().stream()
                .map(this::convertToResponseDTO)
                .collect(Collectors.toList());
    }

    public List<ServiceOrderResponseDTO> getWaitingApproval() {
        return serviceOrderRepository.findWaitingApproval().stream()
                .map(this::convertToResponseDTO)
                .collect(Collectors.toList());
    }

    public List<ServiceOrderResponseDTO> getVehicleHistory(Long vehicleId) {
        return serviceOrderRepository.findVehicleHistory(vehicleId).stream()
                .map(this::convertToResponseDTO)
                .collect(Collectors.toList());
    }

    // Método para converter entidade para DTO
    private ServiceOrderResponseDTO convertToResponseDTO(ServiceOrder serviceOrder) {
        ServiceOrderResponseDTO dto = new ServiceOrderResponseDTO();
        dto.setId(serviceOrder.getId());
        dto.setOrderNumber(serviceOrder.getOrderNumber());
        dto.setVehicleId(serviceOrder.getVehicleId());
        dto.setClientId(serviceOrder.getClientId());
        
        // Dados do veículo
        if (serviceOrder.getVehicle() != null) {
            dto.setVehicleLicensePlate(serviceOrder.getVehicle().getLicensePlate());
            dto.setVehicleModel(serviceOrder.getVehicle().getModel());
            dto.setVehicleBrand(serviceOrder.getVehicle().getBrand());
        }
        
        // Dados do cliente
        if (serviceOrder.getClient() != null) {
            dto.setClientName(serviceOrder.getClient().getName());
            dto.setClientPhone(serviceOrder.getClient().getPhone());
        }
        
        dto.setDescription(serviceOrder.getDescription());
        dto.setProblemDescription(serviceOrder.getProblemDescription());
        dto.setDiagnosis(serviceOrder.getDiagnosis());
        dto.setSolutionDescription(serviceOrder.getSolutionDescription());
        dto.setStatus(serviceOrder.getStatus());
        dto.setLaborCost(serviceOrder.getLaborCost());
        dto.setPartsCost(serviceOrder.getPartsCost());
        dto.setTotalCost(serviceOrder.getTotalCost());
        dto.setDiscount(serviceOrder.getDiscount());
        dto.setFinalCost(serviceOrder.getFinalCost());
        dto.setEstimatedCompletion(serviceOrder.getEstimatedCompletion());
        dto.setActualCompletion(serviceOrder.getActualCompletion());
        dto.setObservations(serviceOrder.getObservations());
        dto.setTechnicianName(serviceOrder.getTechnicianName());
        dto.setCurrentKilometers(serviceOrder.getCurrentKilometers());
        dto.setCreatedAt(serviceOrder.getCreatedAt());
        dto.setUpdatedAt(serviceOrder.getUpdatedAt());
        
        // Converter itens
        if (serviceOrder.getItems() != null) {
            dto.setItems(serviceOrder.getItems().stream()
                    .map(this::convertItemToResponseDTO)
                    .collect(Collectors.toList()));
        }
        
        return dto;
    }

    private ServiceOrderItemResponseDTO convertItemToResponseDTO(ServiceOrderItem item) {
        ServiceOrderItemResponseDTO dto = new ServiceOrderItemResponseDTO();
        dto.setId(item.getId());
        dto.setDescription(item.getDescription());
        dto.setItemType(item.getItemType());
        dto.setQuantity(item.getQuantity());
        dto.setUnitPrice(item.getUnitPrice());
        dto.setTotalPrice(item.getTotalPrice());
        dto.setProductCode(item.getProductCode());
        dto.setRequiresStock(item.getRequiresStock());
        dto.setStockReserved(item.getStockReserved());
        dto.setApplied(item.getApplied());
        dto.setObservations(item.getObservations());
        dto.setCreatedAt(item.getCreatedAt());
        dto.setUpdatedAt(item.getUpdatedAt());
        return dto;
    }
}
