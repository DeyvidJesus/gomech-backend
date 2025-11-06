package com.gomech.service;

import com.gomech.domain.InventoryItem;
import com.gomech.domain.Part;
import com.gomech.dto.Inventory.InventoryEntryRequestDTO;
import com.gomech.dto.Parts.PartCreateDTO;
import com.gomech.dto.Parts.PartMapper;
import com.gomech.dto.Parts.PartResponseDTO;
import com.gomech.dto.Parts.PartUpdateDTO;
import com.gomech.dto.ServiceOrder.ServiceOrderItemCreateDTO;
import com.gomech.dto.ServiceOrder.ServiceOrderItemResponseDTO;
import com.gomech.model.ServiceOrderItemType;
import com.gomech.repository.InventoryItemRepository;
import com.gomech.repository.PartRepository;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.Objects.requireNonNullElse;

@Service
@Transactional
public class PartService {

    private final PartRepository partRepository;
    private final InventoryService inventoryService;
    private final ServiceOrderItemService serviceOrderItemService;
    private final InventoryItemRepository inventoryItemRepository;
    private final AuditService auditService;

    public PartService(PartRepository partRepository,
                       InventoryService inventoryService,
                       ServiceOrderItemService serviceOrderItemService,
                       InventoryItemRepository inventoryItemRepository,
                       AuditService auditService) {
        this.partRepository = partRepository;
        this.inventoryService = inventoryService;
        this.serviceOrderItemService = serviceOrderItemService;
        this.inventoryItemRepository = inventoryItemRepository;
        this.auditService = auditService;
    }

    public PartResponseDTO register(PartCreateDTO dto) {
        Part part = PartMapper.toEntity(dto);
        Part saved = partRepository.save(part);

        registerInitialStockIfRequested(saved, dto);
        linkPartToServiceOrderIfRequested(saved, dto);

        return PartResponseDTO.fromEntity(saved);
    }

    public PartResponseDTO update(Long id, PartUpdateDTO updates) {
        Part existing = partRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Peça não encontrada"));

        PartMapper.updateEntity(existing, updates);

        Part saved = partRepository.save(existing);
        auditService.logEntityAction("UPDATE", "PART", saved.getId(),
                "Peça atualizada: " + saved.getName());
        return PartResponseDTO.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public Optional<PartResponseDTO> getById(Long id) {
        return partRepository.findById(id)
                .map(PartResponseDTO::fromEntity);
    }

    @Transactional(readOnly = true)
    public List<PartResponseDTO> listAll() {
        return partRepository.findAll().stream()
                .map(PartResponseDTO::fromEntity)
                .toList();
    }

    public void delete(Long id) {
        if (!partRepository.existsById(id)) {
            throw new IllegalArgumentException("Peça não encontrada");
        }
        partRepository.deleteById(id);
        auditService.logEntityAction("DELETE", "PART", id,
                "Peça removida");
    }

    private void registerInitialStockIfRequested(Part part, PartCreateDTO dto) {
        if (dto.stockLocation() == null || dto.stockQuantity() == null) {
            return;
        }

        InventoryEntryRequestDTO request = new InventoryEntryRequestDTO(
                part.getId(),
                dto.stockLocation(),
                dto.stockQuantity(),
                requireNonNullElse(dto.stockUnitCost(), dto.unitCost()),
                requireNonNullElse(dto.stockSalePrice(), dto.unitPrice()),
                part.getSku(),
                "Cadastro de peça direcionado ao estoque"
        );

        inventoryService.registerEntry(request);
    }

    private void linkPartToServiceOrderIfRequested(Part part, PartCreateDTO dto) {
        if (dto.serviceOrderId() == null) {
            return;
        }

        ServiceOrderItemCreateDTO itemDto = new ServiceOrderItemCreateDTO();
        itemDto.setPartId(part.getId());
        itemDto.setItemType(ServiceOrderItemType.PART);
        itemDto.setRequiresStock(true);
        itemDto.setDescription(part.getName());
        itemDto.setProductCode(part.getSku());

        if (dto.serviceQuantity() != null) {
            itemDto.setQuantity(dto.serviceQuantity());
        }

        BigDecimal unitPrice = dto.serviceUnitPrice() != null
                ? dto.serviceUnitPrice()
                : requireNonNullElse(part.getUnitPrice(), dto.unitPrice());
        if (unitPrice != null) {
            itemDto.setUnitPrice(unitPrice);
        }

        Long inventoryItemId = resolveInventoryItemId(part, dto);
        if (inventoryItemId == null) {
            throw new IllegalStateException("Não foi possível localizar item de estoque para vincular à ordem de serviço");
        }
        itemDto.setInventoryItemId(inventoryItemId);

        ServiceOrderItemResponseDTO itemResponse = serviceOrderItemService.addItem(dto.serviceOrderId(), itemDto);
        serviceOrderItemService.applyItem(itemResponse.getId());
    }

    private Long resolveInventoryItemId(Part part, PartCreateDTO dto) {
        if (dto.inventoryItemId() != null) {
            return dto.inventoryItemId();
        }

        if (dto.stockLocation() != null) {
            Optional<InventoryItem> byLocation = inventoryItemRepository.findByPartIdAndLocation(part.getId(), dto.stockLocation());
            if (byLocation.isPresent()) {
                return byLocation.get().getId();
            }
        }

        return inventoryItemRepository.findByPartId(part.getId()).stream()
                .findFirst()
                .map(InventoryItem::getId)
                .orElse(null);
    }

    public List<PartResponseDTO> saveFromFile(MultipartFile file) {
        List<Part> parts;
        String filename = file.getOriginalFilename();
        if (filename == null) throw new RuntimeException("Arquivo sem nome");

        if (filename.endsWith(".csv") || filename.endsWith(".txt")) {
            parts = saveFromDelimitedFile(file);
        } else if (filename.endsWith(".xls") || filename.endsWith(".xlsx")) {
            parts = saveFromExcel(file);
        } else {
            throw new RuntimeException("Formato de arquivo não suportado. Use CSV ou XLSX");
        }

        return parts.stream()
                .map(PartResponseDTO::fromEntity)
                .toList();
    }

    private List<Part> saveFromDelimitedFile(MultipartFile file) {
        List<Part> parts = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            // Ignora a primeira linha (cabeçalho)
            String line = reader.readLine();
            
            while ((line = reader.readLine()) != null) {
                String[] fields = line.split(",");
                if (fields.length < 2) continue; // Precisa de pelo menos name e sku
                
                Part part = new Part();
                part.setName(fields[0].trim());
                part.setSku(fields[1].trim());
                if (fields.length > 2 && !fields[2].trim().isEmpty()) part.setManufacturer(fields[2].trim());
                if (fields.length > 3 && !fields[3].trim().isEmpty()) part.setDescription(fields[3].trim());
                if (fields.length > 4 && !fields[4].trim().isEmpty()) {
                    try {
                        part.setUnitCost(new BigDecimal(fields[4].trim()));
                    } catch (NumberFormatException ignored) {}
                }
                if (fields.length > 5 && !fields[5].trim().isEmpty()) {
                    try {
                        part.setUnitPrice(new BigDecimal(fields[5].trim()));
                    } catch (NumberFormatException ignored) {}
                }
                parts.add(part);
            }
        } catch (IOException e) {
            throw new RuntimeException("Falha ao processar arquivo CSV", e);
        }
        
        List<Part> saved = partRepository.saveAll(parts);
        saved.forEach(part -> auditService.logEntityAction("CREATE", "PART", part.getId(),
                "Peça cadastrada via arquivo: " + part.getName()));
        return saved;
    }

    private List<Part> saveFromExcel(MultipartFile file) {
        List<Part> parts = new ArrayList<>();
        try (InputStream is = file.getInputStream(); Workbook workbook = WorkbookFactory.create(is)) {
            DataFormatter formatter = new DataFormatter();
            Sheet sheet = workbook.getSheetAt(0);
            Map<String, Integer> headers = new HashMap<>();
            boolean first = true;
            
            for (Row row : sheet) {
                if (row == null) continue;
                
                if (first) {
                    for (Cell cell : row) {
                        headers.put(formatter.formatCellValue(cell).toLowerCase(), cell.getColumnIndex());
                    }
                    first = false;
                    continue;
                }
                
                // Verificar se a linha está vazia
                boolean isEmptyRow = true;
                for (int i = 0; i < row.getLastCellNum(); i++) {
                    Cell cell = row.getCell(i);
                    if (cell != null && cell.getCellType() != CellType.BLANK) {
                        String value = formatter.formatCellValue(cell).trim();
                        if (!value.isEmpty()) {
                            isEmptyRow = false;
                            break;
                        }
                    }
                }
                
                if (isEmptyRow) continue;
                
                // Processar linha com dados
                Map<String, String> rowData = new HashMap<>();
                for (Map.Entry<String, Integer> entry : headers.entrySet()) {
                    Cell cell = row.getCell(entry.getValue());
                    rowData.put(entry.getKey(), formatter.formatCellValue(cell));
                }
                
                // Verificar campos obrigatórios
                String name = rowData.get("name");
                String sku = rowData.get("sku");
                
                if ((name != null && !name.trim().isEmpty()) && (sku != null && !sku.trim().isEmpty())) {
                    Part part = buildPartFromMap(rowData);
                    parts.add(part);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Falha ao ler planilha", e);
        }
        
        List<Part> saved = partRepository.saveAll(parts);
        saved.forEach(part -> auditService.logEntityAction("CREATE", "PART", part.getId(),
                "Peça cadastrada via planilha: " + part.getName()));
        return saved;
    }

    private Part buildPartFromMap(Map<String, String> data) {
        Part part = new Part();
        
        part.setName(data.get("name"));
        part.setSku(data.get("sku"));
        
        String manufacturer = data.get("manufacturer");
        if (manufacturer != null && !manufacturer.isEmpty()) {
            part.setManufacturer(manufacturer);
        }
        
        String description = data.get("description");
        if (description != null && !description.isEmpty()) {
            part.setDescription(description);
        }
        
        String unitCostStr = data.get("unitcost");
        if (unitCostStr == null) unitCostStr = data.get("unit_cost");
        if (unitCostStr != null && !unitCostStr.isEmpty()) {
            try {
                part.setUnitCost(new BigDecimal(unitCostStr));
            } catch (NumberFormatException ignored) {}
        }
        
        String unitPriceStr = data.get("unitprice");
        if (unitPriceStr == null) unitPriceStr = data.get("unit_price");
        if (unitPriceStr != null && !unitPriceStr.isEmpty()) {
            try {
                part.setUnitPrice(new BigDecimal(unitPriceStr));
            } catch (NumberFormatException ignored) {}
        }
        
        return part;
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
                CSVFormat.DEFAULT.withHeader("name", "sku", "manufacturer", "description", "unitCost", "unitPrice"))) {
            printer.printRecord("Filtro de Óleo", "FO-001", "Bosch", "Filtro de óleo para motor", "25.00", "45.00");
        } catch (IOException e) {
            throw new RuntimeException("Falha ao gerar template CSV", e);
        }
        return new ByteArrayInputStream(out.toByteArray());
    }

    private ByteArrayInputStream generateTemplateExcel() {
        try (Workbook workbook = WorkbookFactory.create(true); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("pecas");
            
            // Estilos
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            
            CellStyle exampleStyle = workbook.createCellStyle();
            exampleStyle.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
            exampleStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            
            // Cabeçalho
            Row header = sheet.createRow(0);
            String[] headers = {"name", "sku", "manufacturer", "description", "unitCost", "unitPrice"};
            String[] headersDesc = {"Nome*", "SKU*", "Fabricante", "Descrição", "Custo Unitário", "Preço de Venda"};
            
            for (int i = 0; i < headers.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(headersDesc[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 5000);
            }
            
            // Linha de exemplo
            Row example = sheet.createRow(1);
            Object[] exampleData = {"Filtro de Óleo", "FO-001", "Bosch", "Filtro de óleo para motor", 25.00, 45.00};
            for (int i = 0; i < exampleData.length; i++) {
                Cell cell = example.createCell(i);
                if (exampleData[i] instanceof Number) {
                    cell.setCellValue(((Number) exampleData[i]).doubleValue());
                } else {
                    cell.setCellValue(String.valueOf(exampleData[i]));
                }
                cell.setCellStyle(exampleStyle);
            }
            
            // Aba de instruções
            Sheet instructionsSheet = workbook.createSheet("Instruções");
            int rowNum = 0;
            
            Row titleRow = instructionsSheet.createRow(rowNum++);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("🔧 INSTRUÇÕES PARA IMPORTAÇÃO DE PEÇAS");
            titleCell.setCellStyle(headerStyle);
            
            rowNum++;
            
            String[] instructions = {
                "1. CAMPOS OBRIGATÓRIOS:",
                "   • name: Nome da peça - OBRIGATÓRIO",
                "   • sku: Código SKU único - OBRIGATÓRIO",
                "",
                "2. FORMATO DOS CAMPOS:",
                "   • name: Texto livre (ex: Filtro de Óleo)",
                "   • sku: Código único alfanumérico (ex: FO-001)",
                "   • manufacturer: Nome do fabricante",
                "   • description: Descrição detalhada",
                "   • unitCost: Valor de custo (ex: 25.00)",
                "   • unitPrice: Preço de venda (ex: 45.00)",
                "",
                "3. DICAS IMPORTANTES:",
                "   • Não altere os nomes das colunas",
                "   • A linha amarela é um exemplo",
                "   • SKU deve ser único no sistema",
                "   • Use ponto para decimais (25.50)",
                "   • Linhas vazias serão ignoradas",
                "",
                "4. APÓS PREENCHER:",
                "   • Salve o arquivo",
                "   • Vá para a tela de Peças",
                "   • Clique em 'Importar'",
                "   • Selecione o arquivo"
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
