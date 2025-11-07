package com.gomech.service;

import com.gomech.dto.Vehicles.VehicleCreateDTO;
import com.gomech.dto.Vehicles.VehicleResponseDTO;
import com.gomech.dto.Vehicles.VehicleUpdateDTO;
import com.gomech.dto.ServiceOrder.ServiceOrderResponseDTO;
import com.gomech.model.Client;
import com.gomech.model.Vehicle;
import com.gomech.model.ServiceOrder;
import com.gomech.repository.ClientRepository;
import com.gomech.repository.VehicleRepository;
import com.gomech.repository.ServiceOrderRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

@Service
public class VehicleService {
    @Autowired
    private VehicleRepository repository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private AuditService auditService;

    @Autowired
    private ServiceOrderRepository serviceOrderRepository;

    @Autowired
    private ServiceOrderService serviceOrderService;

    public VehicleResponseDTO save(VehicleCreateDTO dto) {
        Client client = clientRepository.findById(dto.clientId())
                .orElseThrow(() -> new RuntimeException("Cliente não encontrado"));

        Vehicle vehicle = new Vehicle();
        vehicle.setClient(client);
        vehicle.setBrand(dto.brand());
        vehicle.setLicensePlate(dto.licensePlate());
        vehicle.setModel(dto.model());
        vehicle.setColor(dto.color());
        vehicle.setChassisId(dto.chassisId());
        vehicle.setKilometers(dto.kilometers());
        vehicle.setObservations(dto.observations());
        vehicle.setManufactureDate(dto.manufactureDate());

        Vehicle saved = repository.save(vehicle);
        auditService.logEntityAction("CREATE", "VEHICLE", saved.getId(),
                "Veículo cadastrado: " + saved.getLicensePlate());
        return VehicleResponseDTO.fromEntity(saved);
    }

    public List<VehicleResponseDTO> listAll() {
        return repository.findAll().stream()
                .map(VehicleResponseDTO::fromEntity)
                .toList();
    }

    public Page<VehicleResponseDTO> listAllPaginated(Pageable pageable) {
        return repository.findAll(pageable)
                .map(VehicleResponseDTO::fromEntity);
    }

    public Optional<VehicleResponseDTO> getById(Long id) {
        return repository.findById(id)
                .map(VehicleResponseDTO::fromEntity);
    }

    public void delete(Long id) {
        Vehicle vehicle = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Veículo não encontrado"));
        
        // Verifica se o veículo possui ordens de serviço
        List<ServiceOrder> serviceOrders = serviceOrderRepository.findByVehicleId(id);
        if (!serviceOrders.isEmpty()) {
            throw new IllegalStateException(
                String.format("Não é possível excluir este veículo pois ele possui %d ordem(ns) de serviço associada(s). " +
                              "Remova ou transfira as ordens de serviço antes de excluir o veículo.", serviceOrders.size())
            );
        }
        
        repository.deleteById(id);
        auditService.logEntityAction("DELETE", "VEHICLE", id,
                "Veículo removido: " + vehicle.getLicensePlate());
    }

    public VehicleResponseDTO update(Long id, VehicleUpdateDTO dto) {
        Vehicle vehicle = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Vehicle not found"));

        vehicle.setLicensePlate(dto.licensePlate());
        vehicle.setBrand(dto.brand());
        vehicle.setModel(dto.model());
        vehicle.setColor(dto.color());
        vehicle.setManufactureDate(dto.manufactureDate());
        vehicle.setObservations(dto.observations());
        vehicle.setKilometers(dto.kilometers());
        vehicle.setChassisId(dto.chassisId());

        if (dto.clientId() != null) {
            Client client = clientRepository.findById(dto.clientId())
                    .orElseThrow(() -> new RuntimeException("Cliente não encontrado com ID: " + dto.clientId()));
            vehicle.setClient(client);
        }

        Vehicle saved = repository.save(vehicle);
        auditService.logEntityAction("UPDATE", "VEHICLE", saved.getId(),
                "Veículo atualizado: " + saved.getLicensePlate());
        return VehicleResponseDTO.fromEntity(saved);
    }

    public List<VehicleResponseDTO> saveFromFile(MultipartFile file) {
        List<Vehicle> vehicles;
        String filename = file.getOriginalFilename();
        if (filename == null) throw new RuntimeException("Arquivo sem nome");

        if (filename.endsWith(".csv") || filename.endsWith(".txt")) {
            vehicles = saveFromDelimitedFile(file);
        } else if (filename.endsWith(".xls") || filename.endsWith(".xlsx")) {
            vehicles = saveFromExcel(file);
        } else {
            throw new RuntimeException("Formato de arquivo não suportado");
        }

        return vehicles.stream()
                .map(VehicleResponseDTO::fromEntity)
                .toList();
    }

    private List<Vehicle> saveFromDelimitedFile(MultipartFile file) {
        List<Vehicle> vehicles = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
             CSVParser parser = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(reader)) {
            for (CSVRecord record : parser) {
                Vehicle vehicle = buildVehicleFromMap(record::get);
                vehicles.add(vehicle);
            }
        } catch (IOException e) {
            throw new RuntimeException("Falha ao processar arquivo", e);
        }
        List<Vehicle> saved = repository.saveAll(vehicles);
        saved.forEach(vehicle -> auditService.logEntityAction("CREATE", "VEHICLE", vehicle.getId(),
                "Veículo cadastrado via arquivo: " + vehicle.getLicensePlate()));
        return saved;
    }

    private List<Vehicle> saveFromExcel(MultipartFile file) {
        List<Vehicle> vehicles = new ArrayList<>();
        try (InputStream is = file.getInputStream(); Workbook workbook = WorkbookFactory.create(is)) {
            DataFormatter formatter = new DataFormatter();
            Sheet sheet = workbook.getSheetAt(0);
            Map<String, Integer> headers = new HashMap<>();
            boolean first = true;
            
            for (Row row : sheet) {
                // Pular se a linha for nula
                if (row == null) {
                    continue;
                }
                
                // Primeira linha: ler cabeçalhos
                if (first) {
                    for (Cell cell : row) {
                        headers.put(formatter.formatCellValue(cell).toLowerCase(), cell.getColumnIndex());
                    }
                    first = false;
                    continue;
                }
                
                // Verificar se a linha está completamente vazia
                boolean isEmptyRow = true;
                for (int i = 0; i < row.getLastCellNum(); i++) {
                    Cell cell = row.getCell(i);
                    if (cell != null && cell.getCellType() != org.apache.poi.ss.usermodel.CellType.BLANK) {
                        String value = formatter.formatCellValue(cell).trim();
                        if (!value.isEmpty()) {
                            isEmptyRow = false;
                            break;
                        }
                    }
                }
                
                // Pular linhas vazias
                if (isEmptyRow) {
                    continue;
                }
                
                // Processar linha com dados
                Map<String, String> rowData = new HashMap<>();
                for (Map.Entry<String, Integer> entry : headers.entrySet()) {
                    Cell cell = row.getCell(entry.getValue());
                    rowData.put(entry.getKey(), formatter.formatCellValue(cell));
                }
                
                // Verificar se a linha tem dados mínimos necessários (placa ou chassi)
                String licensePlate = rowData.get("licenseplate");
                if (licensePlate == null) {
                    licensePlate = rowData.get("license_plate");
                }
                String chassisId = rowData.get("chassisid");
                if (chassisId == null) {
                    chassisId = rowData.get("chassis_id");
                }
                
                // Só processar se tiver placa OU chassi
                if ((licensePlate != null && !licensePlate.trim().isEmpty()) || 
                    (chassisId != null && !chassisId.trim().isEmpty())) {
                    Vehicle vehicle = buildVehicleFromMap(rowData::get);
                    vehicles.add(vehicle);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Falha ao ler planilha", e);
        }
        
        List<Vehicle> saved = repository.saveAll(vehicles);
        saved.forEach(vehicle -> auditService.logEntityAction("CREATE", "VEHICLE", vehicle.getId(),
                "Veículo cadastrado via planilha: " + vehicle.getLicensePlate()));
        return saved;
    }

    private Vehicle buildVehicleFromMap(java.util.function.Function<String, String> getter) {
        Vehicle vehicle = new Vehicle();
        
        // Associar cliente (obrigatório)
        String clientIdStr = getter.apply("clientId");
        if (clientIdStr == null) {
            clientIdStr = getter.apply("clientid");
        }
        if (clientIdStr == null) {
            clientIdStr = getter.apply("client_id");
        }
        
        if (clientIdStr != null && !clientIdStr.isEmpty()) {
            try {
                Long clientId = Long.parseLong(clientIdStr.trim());
                Client client = clientRepository.findById(clientId)
                        .orElseThrow(() -> new RuntimeException("Cliente não encontrado com ID: " + clientId));
                vehicle.setClient(client);
            } catch (NumberFormatException e) {
                throw new RuntimeException("ID do cliente inválido: " + clientIdStr);
            }
        } else {
            throw new RuntimeException("O campo clientId é obrigatório para cadastrar um veículo");
        }
        
        vehicle.setLicensePlate(getter.apply("licensePlate"));
        if (vehicle.getLicensePlate() == null) {
            vehicle.setLicensePlate(getter.apply("licenseplate"));
        }
        vehicle.setBrand(getter.apply("brand"));
        vehicle.setModel(getter.apply("model"));
        vehicle.setColor(getter.apply("color"));

        String manufactureDateStr = getter.apply("manufactureDate");
        if (manufactureDateStr == null) {
            manufactureDateStr = getter.apply("manufacturedate");
        }
        if (manufactureDateStr != null && !manufactureDateStr.isEmpty()) {
            LocalDate localDate = LocalDate.parse(manufactureDateStr);
            vehicle.setManufactureDate(java.util.Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant()));
        }

        vehicle.setObservations(getter.apply("observations"));
        String km = getter.apply("kilometers");
        if (km != null && !km.isEmpty()) {
            try {
                vehicle.setKilometers((int) Float.parseFloat(km));
            } catch (NumberFormatException ignored) {}
        }
        vehicle.setChassisId(getter.apply("chassisId"));
        if (vehicle.getChassisId() == null) {
            vehicle.setChassisId(getter.apply("chassisid"));
        }

        return vehicle;
    }

    public ByteArrayInputStream exportToFile(String format) {
        if (format != null && (format.equalsIgnoreCase("xlsx") || format.equalsIgnoreCase("xls"))) {
            return exportToExcel();
        }
        return exportToDelimitedFile();
    }

    private ByteArrayInputStream exportToDelimitedFile() {
        List<Vehicle> vehicles = repository.findAll();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (CSVPrinter printer = new CSVPrinter(new PrintWriter(out),
                CSVFormat.DEFAULT.withHeader("clientId", "licensePlate", "brand", "model", "manufactureDate", "color", "observations", "kilometers", "chassisId"))) {
            for (Vehicle v : vehicles) {
                String manufactureDate = v.getManufactureDate() != null ?
                        LocalDate.ofInstant(v.getManufactureDate().toInstant(), ZoneId.systemDefault()).toString() : "";
                Long clientId = v.getClient() != null ? v.getClient().getId() : null;
                printer.printRecord(clientId, v.getLicensePlate(), v.getBrand(), v.getModel(), manufactureDate, v.getColor(), v.getObservations(), v.getKilometers(), v.getChassisId());
            }
        } catch (IOException e) {
            throw new RuntimeException("Falha ao gerar CSV", e);
        }
        return new ByteArrayInputStream(out.toByteArray());
    }

    private ByteArrayInputStream exportToExcel() {
        List<Vehicle> vehicles = repository.findAll();
        try (Workbook workbook = WorkbookFactory.create(true); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("vehicles");
            String[] headers = {"clientId", "licensePlate", "brand", "model", "manufactureDate", "color", "observations", "kilometers", "chassisId"};
            Row header = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                header.createCell(i).setCellValue(headers[i]);
            }
            int rowIdx = 1;
            for (Vehicle v : vehicles) {
                Row row = sheet.createRow(rowIdx++);
                Long clientId = v.getClient() != null ? v.getClient().getId() : null;
                if (clientId != null) {
                    row.createCell(0).setCellValue(clientId);
                }
                row.createCell(1).setCellValue(v.getLicensePlate());
                row.createCell(2).setCellValue(v.getBrand());
                row.createCell(3).setCellValue(v.getModel());
                String manufactureDate = v.getManufactureDate() != null ?
                        LocalDate.ofInstant(v.getManufactureDate().toInstant(), ZoneId.systemDefault()).toString() : "";
                row.createCell(4).setCellValue(manufactureDate);
                row.createCell(5).setCellValue(v.getColor());
                row.createCell(6).setCellValue(v.getObservations());
                row.createCell(7).setCellValue(v.getKilometers());
                row.createCell(8).setCellValue(v.getChassisId());
            }
            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException("Falha ao gerar planilha", e);
        }
    }

    public List<ServiceOrderResponseDTO> getServiceHistory(Long vehicleId) {
        // Verifica se o veículo existe
        repository.findById(vehicleId)
                .orElseThrow(() -> new RuntimeException("Veículo não encontrado"));
        
        // Busca o histórico de ordens de serviço do veículo usando o service
        return serviceOrderService.getVehicleHistory(vehicleId);
    }

    public ByteArrayInputStream exportServiceHistory(Long vehicleId, String format) {
        Vehicle vehicle = repository.findById(vehicleId)
                .orElseThrow(() -> new RuntimeException("Veículo não encontrado"));
        
        List<ServiceOrder> serviceOrders = serviceOrderRepository.findVehicleHistory(vehicleId);
        
        if (format != null && (format.equalsIgnoreCase("xlsx") || format.equalsIgnoreCase("xls"))) {
            return exportServiceHistoryToExcel(vehicle, serviceOrders);
        }
        return exportServiceHistoryToCSV(vehicle, serviceOrders);
    }

    private ByteArrayInputStream exportServiceHistoryToCSV(Vehicle vehicle, List<ServiceOrder> serviceOrders) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (CSVPrinter printer = new CSVPrinter(new PrintWriter(out),
                CSVFormat.DEFAULT.withHeader(
                    "Número OS", "Data Criação", "Data Conclusão", "Status", 
                    "Descrição", "Problema", "Diagnóstico", "Solução", 
                    "Técnico", "Quilometragem", "Custo Mão de Obra", "Custo Peças", 
                    "Desconto", "Custo Total", "Observações"))) {
            
            for (ServiceOrder so : serviceOrders) {
                printer.printRecord(
                    so.getOrderNumber(),
                    so.getCreatedAt() != null ? so.getCreatedAt().toString() : "",
                    so.getActualCompletion() != null ? so.getActualCompletion().toString() : "",
                    so.getStatus() != null ? so.getStatus().toString() : "",
                    so.getDescription() != null ? so.getDescription() : "",
                    so.getProblemDescription() != null ? so.getProblemDescription() : "",
                    so.getDiagnosis() != null ? so.getDiagnosis() : "",
                    so.getSolutionDescription() != null ? so.getSolutionDescription() : "",
                    so.getTechnicianName() != null ? so.getTechnicianName() : "",
                    so.getCurrentKilometers() != null ? so.getCurrentKilometers().toString() : "",
                    so.getLaborCost() != null ? so.getLaborCost().toString() : "0",
                    so.getPartsCost() != null ? so.getPartsCost().toString() : "0",
                    so.getDiscount() != null ? so.getDiscount().toString() : "0",
                    so.getTotalCost() != null ? so.getTotalCost().toString() : "0",
                    so.getObservations() != null ? so.getObservations() : ""
                );
            }
        } catch (IOException e) {
            throw new RuntimeException("Falha ao gerar CSV do histórico", e);
        }
        return new ByteArrayInputStream(out.toByteArray());
    }

    private ByteArrayInputStream exportServiceHistoryToExcel(Vehicle vehicle, List<ServiceOrder> serviceOrders) {
        try (Workbook workbook = WorkbookFactory.create(true); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Histórico de Serviços");
            
            // Estilo para cabeçalho
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            
            // Informações do veículo
            Row vehicleInfoRow = sheet.createRow(0);
            Cell vehicleInfoCell = vehicleInfoRow.createCell(0);
            vehicleInfoCell.setCellValue("Veículo: " + vehicle.getBrand() + " " + vehicle.getModel() + " - Placa: " + vehicle.getLicensePlate());
            vehicleInfoCell.setCellStyle(headerStyle);
            
            // Linha vazia
            sheet.createRow(1);
            
            // Cabeçalhos
            String[] headers = {
                "Número OS", "Data Criação", "Data Conclusão", "Status", 
                "Descrição", "Problema", "Diagnóstico", "Solução", 
                "Técnico", "Quilometragem", "Custo Mão de Obra", "Custo Peças", 
                "Desconto", "Custo Total", "Observações"
            };
            Row headerRow = sheet.createRow(2);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }
            
            // Dados
            int rowIdx = 3;
            for (ServiceOrder so : serviceOrders) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(so.getOrderNumber());
                row.createCell(1).setCellValue(so.getCreatedAt() != null ? so.getCreatedAt().toString() : "");
                row.createCell(2).setCellValue(so.getActualCompletion() != null ? so.getActualCompletion().toString() : "");
                row.createCell(3).setCellValue(so.getStatus() != null ? so.getStatus().toString() : "");
                row.createCell(4).setCellValue(so.getDescription() != null ? so.getDescription() : "");
                row.createCell(5).setCellValue(so.getProblemDescription() != null ? so.getProblemDescription() : "");
                row.createCell(6).setCellValue(so.getDiagnosis() != null ? so.getDiagnosis() : "");
                row.createCell(7).setCellValue(so.getSolutionDescription() != null ? so.getSolutionDescription() : "");
                row.createCell(8).setCellValue(so.getTechnicianName() != null ? so.getTechnicianName() : "");
                row.createCell(9).setCellValue(so.getCurrentKilometers() != null ? so.getCurrentKilometers().toString() : "");
                row.createCell(10).setCellValue(so.getLaborCost() != null ? so.getLaborCost().toString() : "0");
                row.createCell(11).setCellValue(so.getPartsCost() != null ? so.getPartsCost().toString() : "0");
                row.createCell(12).setCellValue(so.getDiscount() != null ? so.getDiscount().toString() : "0");
                row.createCell(13).setCellValue(so.getTotalCost() != null ? so.getTotalCost().toString() : "0");
                row.createCell(14).setCellValue(so.getObservations() != null ? so.getObservations() : "");
            }
            
            // Auto-ajustar largura das colunas
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }
            
            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException("Falha ao gerar planilha do histórico", e);
        }
    }

    public ByteArrayInputStream generateTemplate(String format) {
        if (format != null && (format.equalsIgnoreCase("xlsx") || format.equalsIgnoreCase("xls"))) {
            return generateTemplateExcel();
        }
        return generateTemplateCsv();
    }

    private ByteArrayInputStream generateTemplateCsv() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (CSVPrinter printer = new CSVPrinter(new PrintWriter(out),
                CSVFormat.DEFAULT.withHeader("clientId", "licensePlate", "brand", "model", "manufactureDate", "color", "observations", "kilometers", "chassisId"))) {
            // Adiciona uma linha de exemplo
            printer.printRecord("1", "ABC-1234", "Toyota", "Corolla", "2020-01-01", "Prata", "Veículo revisado", "50000", "9BWZZZ377VT004251");
        } catch (IOException e) {
            throw new RuntimeException("Falha ao gerar template CSV", e);
        }
        return new ByteArrayInputStream(out.toByteArray());
    }

    private ByteArrayInputStream generateTemplateExcel() {
        try (Workbook workbook = WorkbookFactory.create(true); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("veiculos");
            
            // Criar estilos
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.ORANGE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            
            CellStyle exampleStyle = workbook.createCellStyle();
            exampleStyle.setFillForegroundColor(IndexedColors.WHITE.getIndex());
            exampleStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            
            // Cabeçalho
            Row header = sheet.createRow(0);
            String[] headers = {"clientId", "licensePlate", "brand", "model", "manufactureDate", "color", "observations", "kilometers", "chassisId"};
            String[] headersDesc = {"ID do Cliente*", "Placa*", "Marca*", "Modelo*", "Data Fabricação*", "Cor", "Observações", "Quilometragem", "Chassi"};
            
            for (int i = 0; i < headers.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(headersDesc[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 4000);
            }
            
            // Linha de exemplo
            Row example = sheet.createRow(1);
            Object[] exampleData = {1, "ABC-1234", "Toyota", "Corolla", "2020-01-01", "Prata", "Veículo revisado", 50000, "9BWZZZ377VT004251"};
            for (int i = 0; i < exampleData.length; i++) {
                Cell cell = example.createCell(i);
                if (exampleData[i] instanceof Number) {
                    cell.setCellValue(((Number) exampleData[i]).doubleValue());
                } else {
                    cell.setCellValue(String.valueOf(exampleData[i]));
                }
                cell.setCellStyle(exampleStyle);
            }
            
            // Adicionar instruções em uma aba separada
            Sheet instructionsSheet = workbook.createSheet("Instruções");
            int rowNum = 0;
            
            Row titleRow = instructionsSheet.createRow(rowNum++);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("🚗 INSTRUÇÕES PARA IMPORTAÇÃO DE VEÍCULOS");
            titleCell.setCellStyle(headerStyle);
            
            rowNum++; // linha em branco
            
            String[] instructions = {
                "1. CAMPOS OBRIGATÓRIOS:",
                "   • clientId: ID do cliente (número) - OBRIGATÓRIO",
                "   • licensePlate: Placa do veículo (ex: ABC-1234) - mínimo obrigatório: placa OU chassi",
                "   • brand: Marca do veículo",
                "   • model: Modelo do veículo",
                "   • manufactureDate: Data de fabricação",
                "",
                "2. FORMATO DOS CAMPOS:",
                "   • clientId: Número inteiro (ID do cliente existente no sistema)",
                "   • licensePlate: Texto, padrão brasileiro ABC-1234 ou ABC1D234",
                "   • brand: Texto livre (ex: Toyota, Volkswagen, Ford)",
                "   • model: Texto livre (ex: Corolla, Gol, Fiesta)",
                "   • manufactureDate: Formato YYYY-MM-DD (ex: 2020-01-01)",
                "   • color: Texto livre (ex: Prata, Preto, Branco)",
                "   • observations: Texto livre para observações",
                "   • kilometers: Número inteiro (quilometragem atual)",
                "   • chassisId: Código do chassi (17 caracteres)",
                "",
                "3. DICAS IMPORTANTES:",
                "   • Não altere os nomes das colunas na primeira linha",
                "   • A linha amarela é apenas um exemplo, pode ser removida",
                "   • Linhas completamente vazias serão ignoradas",
                "   • O clientId DEVE ser de um cliente existente no sistema",
                "   • Pelo menos um dos campos (placa ou chassi) deve estar preenchido",
                "   • Datas devem estar no formato YYYY-MM-DD",
                "",
                "4. COMO OBTER O ID DO CLIENTE:",
                "   • Vá para a tela de Clientes",
                "   • Exporte a lista de clientes",
                "   • Use o ID da primeira coluna na planilha de veículos",
                "",
                "5. APÓS PREENCHER:",
                "   • Salve o arquivo",
                "   • Vá para a tela de Veículos",
                "   • Clique em 'Importar Planilha'",
                "   • Selecione o arquivo e faça o upload"
            };
            
            for (String instruction : instructions) {
                Row row = instructionsSheet.createRow(rowNum++);
                row.createCell(0).setCellValue(instruction);
            }
            
            instructionsSheet.setColumnWidth(0, 20000);
            
            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException("Falha ao gerar template", e);
        }
    }
}
