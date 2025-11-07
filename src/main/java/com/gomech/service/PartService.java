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
import com.gomech.repository.ServiceOrderItemRepository;
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
    private final ServiceOrderItemRepository serviceOrderItemRepository;
    private final AuditService auditService;

    public PartService(PartRepository partRepository,
                       InventoryService inventoryService,
                       ServiceOrderItemService serviceOrderItemService,
                       InventoryItemRepository inventoryItemRepository,
                       ServiceOrderItemRepository serviceOrderItemRepository,
                       AuditService auditService) {
        this.partRepository = partRepository;
        this.inventoryService = inventoryService;
        this.serviceOrderItemService = serviceOrderItemService;
        this.inventoryItemRepository = inventoryItemRepository;
        this.serviceOrderItemRepository = serviceOrderItemRepository;
        this.auditService = auditService;
    }

    public PartResponseDTO register(PartCreateDTO dto) {
        Part part = PartMapper.toEntity(dto);
        
        // Gera SKU automaticamente se não foi fornecido
        if (part.getSku() == null || part.getSku().trim().isEmpty()) {
            part.setSku(generateUniqueSku(part.getName()));
        }
        
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
        
        // Verifica se a peça está sendo usada em alguma ordem de serviço
        long usageCount = serviceOrderItemRepository.countByPartId(id);
        if (usageCount > 0) {
            throw new IllegalStateException(
                String.format("Não é possível excluir esta peça pois ela está sendo usada em %d ordem(ns) de serviço. " +
                              "Remova ou substitua a peça nas ordens de serviço antes de excluí-la.", usageCount)
            );
        }
        
        // Verifica se a peça tem itens de estoque
        List<InventoryItem> inventoryItems = inventoryItemRepository.findByPartId(id);
        if (!inventoryItems.isEmpty()) {
            throw new IllegalStateException(
                "Não é possível excluir esta peça pois ela possui itens no estoque. " +
                "Remova os itens do estoque antes de excluir a peça."
            );
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

    /**
     * Gera um SKU único baseado no nome da peça.
     * Formato: PREFIX-XXXXX onde PREFIX são as primeiras letras do nome e XXXXX é um número sequencial.
     */
    private String generateUniqueSku(String partName) {
        // Gera prefixo a partir do nome (primeiras letras de cada palavra, máximo 4 caracteres)
        String prefix = generateSkuPrefix(partName);
        
        // Busca o último SKU com esse prefixo para incrementar
        String basePattern = prefix + "-%";
        List<Part> existingParts = partRepository.findAll().stream()
                .filter(p -> p.getSku() != null && p.getSku().startsWith(prefix + "-"))
                .toList();
        
        int maxNumber = 0;
        for (Part p : existingParts) {
            try {
                String[] parts = p.getSku().split("-");
                if (parts.length >= 2) {
                    int num = Integer.parseInt(parts[parts.length - 1]);
                    if (num > maxNumber) {
                        maxNumber = num;
                    }
                }
            } catch (NumberFormatException ignored) {
                // Ignora SKUs que não seguem o padrão
            }
        }
        
        // Gera o novo SKU com número incrementado
        int nextNumber = maxNumber + 1;
        
        // Verifica se já existe (por segurança) e incrementa se necessário
        String newSku;
        List<Part> allParts = partRepository.findAll();
        do {
            newSku = String.format("%s-%05d", prefix, nextNumber);
            final String checkSku = newSku;
            if (allParts.stream().noneMatch(p -> checkSku.equals(p.getSku()))) {
                break;
            }
            nextNumber++;
        } while (true);
        
        return newSku;
    }

    /**
     * Gera o prefixo do SKU a partir do nome da peça.
     * Extrai as primeiras letras de cada palavra (máximo 4 caracteres).
     */
    private String generateSkuPrefix(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "PART";
        }
        
        // Remove caracteres especiais e divide em palavras
        String cleanName = name.trim().toUpperCase()
                .replaceAll("[^A-Z0-9\\s]", "")
                .replaceAll("\\s+", " ");
        
        String[] words = cleanName.split(" ");
        StringBuilder prefix = new StringBuilder();
        
        // Pega a primeira letra de cada palavra
        for (String word : words) {
            if (!word.isEmpty() && prefix.length() < 4) {
                prefix.append(word.charAt(0));
            }
        }
        
        // Se o prefixo for muito curto, completa com as próximas letras da primeira palavra
        if (prefix.length() < 3 && words.length > 0) {
            String firstWord = words[0];
            for (int i = 1; i < firstWord.length() && prefix.length() < 4; i++) {
                prefix.append(firstWord.charAt(i));
            }
        }
        
        // Se ainda estiver vazio, usa valor padrão
        if (prefix.length() == 0) {
            return "PART";
        }
        
        return prefix.toString();
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
                if (fields.length < 1) continue; // Precisa de pelo menos name
                
                Part part = new Part();
                String name = fields[0].trim();
                if (name.isEmpty()) continue;
                
                part.setName(name);
                
                // SKU é opcional - será gerado se não fornecido
                if (fields.length > 1 && !fields[1].trim().isEmpty()) {
                    part.setSku(fields[1].trim());
                } else {
                    part.setSku(generateUniqueSku(name));
                }
                
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
                
                // Define como ativo por padrão
                part.setActive(true);
                
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
                
                // Verificar campos obrigatórios - apenas name é obrigatório agora
                String name = rowData.get("name");
                
                if (name != null && !name.trim().isEmpty()) {
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
        
        String name = data.get("name");
        part.setName(name);
        
        // SKU é opcional - será gerado se não fornecido
        String sku = data.get("sku");
        if (sku != null && !sku.trim().isEmpty()) {
            part.setSku(sku.trim());
        } else {
            part.setSku(generateUniqueSku(name));
        }
        
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
        
        // Define como ativo por padrão
        part.setActive(true);
        
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
            printer.printRecord("Filtro de Óleo", "", "Bosch", "Filtro de óleo para motor", "25.00", "45.00");
            printer.printRecord("Pastilha de Freio", "PF-001", "Fremax", "Pastilha de freio dianteira", "80.00", "150.00");
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
            String[] headersDesc = {"Nome*", "SKU (auto)", "Fabricante", "Descrição", "Custo Unitário", "Preço de Venda"};
            
            for (int i = 0; i < headers.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(headersDesc[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 5000);
            }
            
            // Linha de exemplo 1 - com SKU gerado automaticamente
            Row example1 = sheet.createRow(1);
            Object[] exampleData1 = {"Filtro de Óleo", "", "Bosch", "Filtro de óleo para motor", 25.00, 45.00};
            for (int i = 0; i < exampleData1.length; i++) {
                Cell cell = example1.createCell(i);
                if (exampleData1[i] instanceof Number) {
                    cell.setCellValue(((Number) exampleData1[i]).doubleValue());
                } else {
                    cell.setCellValue(String.valueOf(exampleData1[i]));
                }
                cell.setCellStyle(exampleStyle);
            }
            
            // Linha de exemplo 2 - com SKU manual
            Row example2 = sheet.createRow(2);
            Object[] exampleData2 = {"Pastilha de Freio", "PF-001", "Fremax", "Pastilha de freio dianteira", 80.00, 150.00};
            for (int i = 0; i < exampleData2.length; i++) {
                Cell cell = example2.createCell(i);
                if (exampleData2[i] instanceof Number) {
                    cell.setCellValue(((Number) exampleData2[i]).doubleValue());
                } else {
                    cell.setCellValue(String.valueOf(exampleData2[i]));
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
                "",
                "2. CAMPOS OPCIONAIS:",
                "   • sku: Código SKU único - OPCIONAL (será gerado automaticamente se não fornecido)",
                "   • manufacturer: Nome do fabricante",
                "   • description: Descrição detalhada",
                "   • unitCost: Valor de custo (ex: 25.00)",
                "   • unitPrice: Preço de venda (ex: 45.00)",
                "",
                "3. GERAÇÃO AUTOMÁTICA DE SKU:",
                "   • Se você deixar o campo SKU vazio, o sistema gerará automaticamente",
                "   • O SKU gerado usa as iniciais do nome + número sequencial",
                "   • Exemplo: 'Filtro de Óleo' → 'FDO-00001'",
                "   • Você também pode fornecer seu próprio SKU único",
                "",
                "4. FORMATO DOS CAMPOS:",
                "   • name: Texto livre (ex: Filtro de Óleo)",
                "   • sku: Código único alfanumérico (ex: FO-001) ou vazio para auto-gerar",
                "   • Use ponto para decimais (25.50)",
                "",
                "5. DICAS IMPORTANTES:",
                "   • Não altere os nomes das colunas",
                "   • As linhas amarelas são exemplos",
                "   • Se fornecer SKU, ele deve ser único no sistema",
                "   • Linhas vazias serão ignoradas",
                "",
                "6. APÓS PREENCHER:",
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
