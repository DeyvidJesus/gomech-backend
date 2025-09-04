package com.gomech.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gomech.service.PythonAiService;
// IngestionService removido - DTOs movidos para este controller
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

@RestController
@RequestMapping("/api/files")
public class FileIngestionController {
    
    private static final Logger log = LoggerFactory.getLogger(FileIngestionController.class);
    private final PythonAiService pythonAiService;
    
    public FileIngestionController(PythonAiService pythonAiService) {
        this.pythonAiService = pythonAiService;
    }

    @PostMapping("/upload")
    public ResponseEntity<FileUploadResponse> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "collectionName", defaultValue = "user_upload") String collectionName) {
        
        try {
            log.info("Recebendo arquivo: {} (tamanho: {} bytes)", file.getOriginalFilename(), file.getSize());
            
            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new FileUploadResponse("error", "Arquivo está vazio", 0));
            }
            
            // Processa arquivo baseado na extensão
            List<Map<String, Object>> data = processFile(file);
            
            if (data.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new FileUploadResponse("error", "Nenhum dado válido encontrado no arquivo", 0));
            }
            
            // Cria request para ingestão
            IngestionRequest request = new IngestionRequest();
            request.setData(data);
            request.setCollectionName(collectionName);
            
            // Envia para Python AI Service
            IngestionResponse response = pythonAiService.ingestData(request);
            
            if (response != null && "success".equals(response.getStatus())) {
                log.info("✅ Arquivo processado com sucesso: {} documentos", response.getDocumentsProcessed());
                return ResponseEntity.ok(new FileUploadResponse(
                    "success", 
                    "Arquivo processado e indexado com sucesso", 
                    response.getDocumentsProcessed()
                ));
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(new FileUploadResponse("error", "Erro na indexação dos dados", 0));
            }
            
        } catch (Exception e) {
            log.error("❌ Erro ao processar arquivo: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new FileUploadResponse("error", "Erro ao processar arquivo: " + e.getMessage(), 0));
        }
    }
    
    @PostMapping("/clear")
    public ResponseEntity<Map<String, String>> clearData() {
        try {
            boolean success = pythonAiService.clearVectorStore();
            
            if (success) {
                return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Dados limpos com sucesso"
                ));
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("status", "error", "message", "Erro ao limpar dados"));
            }
            
        } catch (Exception e) {
            log.error("Erro ao limpar dados: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", "error", "message", e.getMessage()));
        }
    }
    
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        try {
            Map<String, Object> status = pythonAiService.getIngestionStatus();
            return ResponseEntity.ok(status);
            
        } catch (Exception e) {
            log.error("Erro ao obter status: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    private List<Map<String, Object>> processFile(MultipartFile file) throws IOException {
        String filename = file.getOriginalFilename();
        if (filename == null) {
            throw new IllegalArgumentException("Nome do arquivo não pode ser nulo");
        }
        
        String extension = filename.toLowerCase();
        
        if (extension.endsWith(".json")) {
            return processJsonFile(file);
        } else if (extension.endsWith(".csv")) {
            return processCsvFile(file);
        } else if (extension.endsWith(".xlsx") || extension.endsWith(".xls")) {
            return processExcelFile(file);
        } else {
            throw new IllegalArgumentException("Formato de arquivo não suportado. Use: JSON, CSV, XLS ou XLSX");
        }
    }
    
    private List<Map<String, Object>> processJsonFile(MultipartFile file) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(file.getInputStream());
        
        List<Map<String, Object>> data = new ArrayList<>();
        
        if (root.isArray()) {
            // Array de objetos
            for (JsonNode item : root) {
                data.add(mapper.convertValue(item, Map.class));
            }
        } else if (root.isObject()) {
            // Objeto único
            data.add(mapper.convertValue(root, Map.class));
        }
        
        return data;
    }
    
    private List<Map<String, Object>> processCsvFile(MultipartFile file) throws IOException {
        List<Map<String, Object>> data = new ArrayList<>();
        
        try (InputStreamReader reader = new InputStreamReader(file.getInputStream());
             CSVParser parser = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(reader)) {
            
            for (CSVRecord record : parser) {
                Map<String, Object> item = new HashMap<>();
                for (String header : parser.getHeaderNames()) {
                    item.put(header, record.get(header));
                }
                data.add(item);
            }
        }
        
        return data;
    }
    
    private List<Map<String, Object>> processExcelFile(MultipartFile file) throws IOException {
        List<Map<String, Object>> data = new ArrayList<>();
        
        Workbook workbook;
        String filename = file.getOriginalFilename();
        
        if (filename != null && filename.endsWith(".xlsx")) {
            workbook = new XSSFWorkbook(file.getInputStream());
        } else {
            workbook = new HSSFWorkbook(file.getInputStream());
        }
        
        try {
            Sheet sheet = workbook.getSheetAt(0); // Primeira aba
            Iterator<Row> rowIterator = sheet.iterator();
            
            // Primeira linha como cabeçalho
            Row headerRow = rowIterator.next();
            List<String> headers = new ArrayList<>();
            
            for (Cell cell : headerRow) {
                headers.add(getCellValueAsString(cell));
            }
            
            // Processa dados
            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                Map<String, Object> item = new HashMap<>();
                
                for (int i = 0; i < headers.size() && i < row.getLastCellNum(); i++) {
                    Cell cell = row.getCell(i);
                    String value = getCellValueAsString(cell);
                    item.put(headers.get(i), value);
                }
                
                data.add(item);
            }
            
        } finally {
            workbook.close();
        }
        
        return data;
    }
    
    private String getCellValueAsString(Cell cell) {
        if (cell == null) return "";
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    return String.valueOf(cell.getNumericCellValue());
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return "";
        }
    }
    
    public static class FileUploadResponse {
        private String status;
        private String message;
        private int documentsProcessed;
        
        public FileUploadResponse(String status, String message, int documentsProcessed) {
            this.status = status;
            this.message = message;
            this.documentsProcessed = documentsProcessed;
        }
        
        // Getters
        public String getStatus() { return status; }
        public String getMessage() { return message; }
        public int getDocumentsProcessed() { return documentsProcessed; }
    }

    // Classes para comunicação com Python AI Service
    public static class IngestionRequest {
        private List<Map<String, Object>> data;
        private String collectionName;

        public List<Map<String, Object>> getData() { return data; }
        public void setData(List<Map<String, Object>> data) { this.data = data; }
        public String getCollectionName() { return collectionName; }
        public void setCollectionName(String collectionName) { this.collectionName = collectionName; }
    }

    public static class IngestionResponse {
        private String status;
        private String message;
        private int documentsProcessed;

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public int getDocumentsProcessed() { return documentsProcessed; }
        public void setDocumentsProcessed(int documentsProcessed) { this.documentsProcessed = documentsProcessed; }
    }
}
