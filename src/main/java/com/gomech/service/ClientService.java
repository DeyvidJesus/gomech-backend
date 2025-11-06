package com.gomech.service;

import com.gomech.dto.Clients.ClientCreateDTO;
import com.gomech.dto.Clients.ClientUpdateDTO;
import com.gomech.model.Client;
import com.gomech.repository.ClientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.time.LocalDate;
import java.time.ZoneId;

@Service
public class ClientService {
    @Autowired
    private ClientRepository repository;

    @Autowired
    private AuditService auditService;

    public Client save(ClientCreateDTO dto) {
        Client client = new Client();
        client.setName(dto.name());
        client.setDocument(dto.document());
        client.setPhone(dto.phone());
        client.setEmail(dto.email());
        client.setAddress(dto.address());
        client.setBirthDate(dto.birthDate());
        client.setObservations(dto.observations());

        Client saved = repository.save(client);
        auditService.logEntityAction("CREATE", "CLIENT", saved.getId(),
                "Cliente cadastrado: " + saved.getName());
        return saved;
    }

    public List<Client> listAll() {
        return repository.findAll();
    }

    public Page<Client> listAllPaginated(Pageable pageable) {
        return repository.findAll(pageable);
    }

    public Optional<Client> getById(Long id) {
        return repository.findById(id);
    }

    public void delete(Long id) {
        repository.deleteById(id);
        auditService.logEntityAction("DELETE", "CLIENT", id,
                "Cliente removido");
    }

    public Client update(Long id, ClientUpdateDTO updatedClient) {
        Client client = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Client not found"));
        client.setName(updatedClient.name());
        client.setDocument(updatedClient.document());
        client.setEmail(updatedClient.email());
        client.setPhone(updatedClient.phone());
        client.setAddress(updatedClient.address());
        client.setBirthDate(updatedClient.birthDate());
        client.setObservations(updatedClient.observations());
        Client saved = repository.save(client);
        auditService.logEntityAction("UPDATE", "CLIENT", saved.getId(),
                "Cliente atualizado: " + saved.getName());
        return saved;
    }

    public List<Client> saveAll(List<Client> clients) {
        List<Client> savedClients = repository.saveAll(clients);
        savedClients.forEach(client -> auditService.logEntityAction("CREATE", "CLIENT", client.getId(),
                "Cliente cadastrado em lote: " + client.getName()));
        return savedClients;
    }

    public List<Client> saveFromFile(MultipartFile file) {
        String filename = file.getOriginalFilename();
        if (filename == null) {
            throw new RuntimeException("Arquivo sem nome");
        }

        if (filename.endsWith(".csv") || filename.endsWith(".txt")) {
            return saveFromDelimitedFile(file);
        } else if (filename.endsWith(".xls") || filename.endsWith(".xlsx")) {
            return saveFromExcel(file);
        } else {
            throw new RuntimeException("Formato de arquivo não suportado");
        }
    }

    private List<Client> saveFromDelimitedFile(MultipartFile file) {
        List<Client> clients = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
             CSVParser parser = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(reader)) {
            for (CSVRecord record : parser) {
                // Verificar se o registro tem dados mínimos necessários
                String name = record.get("name");
                String document = record.get("document");
                String email = record.get("email");
                
                // Só processar se tiver pelo menos nome OU documento OU email
                if ((name != null && !name.trim().isEmpty()) || 
                    (document != null && !document.trim().isEmpty()) ||
                    (email != null && !email.trim().isEmpty())) {
                    Client client = buildClientFromMap(record::get);
                    clients.add(client);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Falha ao processar arquivo", e);
        }
        List<Client> saved = repository.saveAll(clients);
        saved.forEach(client -> auditService.logEntityAction("CREATE", "CLIENT", client.getId(),
                "Cliente cadastrado via arquivo: " + client.getName()));
        return saved;
    }

    private List<Client> saveFromExcel(MultipartFile file) {
        List<Client> clients = new ArrayList<>();
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
                
                // Verificar se a linha tem dados mínimos necessários
                String name = rowData.get("name");
                String document = rowData.get("document");
                String email = rowData.get("email");
                
                // Só processar se tiver pelo menos nome OU documento OU email
                if ((name != null && !name.trim().isEmpty()) || 
                    (document != null && !document.trim().isEmpty()) ||
                    (email != null && !email.trim().isEmpty())) {
                    Client client = buildClientFromMap(rowData::get);
                    clients.add(client);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Falha ao ler planilha", e);
        }
        
        List<Client> saved = repository.saveAll(clients);
        saved.forEach(client -> auditService.logEntityAction("CREATE", "CLIENT", client.getId(),
                "Cliente cadastrado via planilha: " + client.getName()));
        return saved;
    }

    private Client buildClientFromMap(java.util.function.Function<String, String> getter) {
        Client client = new Client();
        client.setName(getter.apply("name"));
        client.setDocument(getter.apply("document"));
        client.setPhone(getter.apply("phone"));
        client.setEmail(getter.apply("email"));
        client.setAddress(getter.apply("address"));

        String birthDateStr = getter.apply("birthDate");
        if (birthDateStr == null) {
            birthDateStr = getter.apply("birthdate");
        }
        if (birthDateStr != null && !birthDateStr.isEmpty()) {
            LocalDate localDate = LocalDate.parse(birthDateStr);
            client.setBirthDate(java.util.Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant()));
        }

        client.setObservations(getter.apply("observations"));
        return client;
    }

    public ByteArrayInputStream exportToFile(String format) {
        if (format != null && (format.equalsIgnoreCase("xlsx") || format.equalsIgnoreCase("xls"))) {
            return exportToExcel();
        }
        return exportToDelimitedFile();
    }

    private ByteArrayInputStream exportToDelimitedFile() {
        List<Client> clients = repository.findAll();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (CSVPrinter printer = new CSVPrinter(new PrintWriter(out),
                CSVFormat.DEFAULT.withHeader("name", "document", "phone", "email", "address", "birthDate", "observations"))) {
            for (Client c : clients) {
                String birthDate = c.getBirthDate() != null ?
                        LocalDate.ofInstant(c.getBirthDate().toInstant(), ZoneId.systemDefault()).toString() : "";
                printer.printRecord(c.getName(), c.getDocument(), c.getPhone(), c.getEmail(), c.getAddress(), birthDate, c.getObservations());
            }
        } catch (IOException e) {
            throw new RuntimeException("Falha ao gerar CSV", e);
        }
        return new ByteArrayInputStream(out.toByteArray());
    }

    private ByteArrayInputStream exportToExcel() {
        List<Client> clients = repository.findAll();
        try (Workbook workbook = WorkbookFactory.create(true); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("clients");
            Row header = sheet.createRow(0);
            String[] headers = {"name", "document", "phone", "email", "address", "birthDate", "observations"};
            for (int i = 0; i < headers.length; i++) {
                header.createCell(i).setCellValue(headers[i]);
            }
            int rowIdx = 1;
            for (Client c : clients) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(c.getName());
                row.createCell(1).setCellValue(c.getDocument());
                row.createCell(2).setCellValue(c.getPhone());
                row.createCell(3).setCellValue(c.getEmail());
                row.createCell(4).setCellValue(c.getAddress());
                String birthDate = c.getBirthDate() != null ?
                        LocalDate.ofInstant(c.getBirthDate().toInstant(), ZoneId.systemDefault()).toString() : "";
                row.createCell(5).setCellValue(birthDate);
                row.createCell(6).setCellValue(c.getObservations());
            }
            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException("Falha ao gerar planilha", e);
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
                CSVFormat.DEFAULT.withHeader("name", "document", "phone", "email", "address", "birthDate", "observations"))) {
            printer.printRecord("João da Silva", "12345678900", "(11) 98765-4321", "joao@email.com", "Rua Exemplo, 123", "1990-01-15", "Cliente VIP");
        } catch (IOException e) {
            throw new RuntimeException("Falha ao gerar template CSV", e);
        }
        return new ByteArrayInputStream(out.toByteArray());
    }

    private ByteArrayInputStream generateTemplateExcel() {
        try (Workbook workbook = WorkbookFactory.create(true); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("clientes");
            
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
            String[] headers = {"name", "document", "phone", "email", "address", "birthDate", "observations"};
            String[] headersDesc = {"Nome*", "CPF/CNPJ", "Telefone", "Email", "Endereço", "Data Nascimento", "Observações"};
            
            for (int i = 0; i < headers.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(headersDesc[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 4000);
            }
            
            // Linha de exemplo
            Row example = sheet.createRow(1);
            String[] exampleData = {"João da Silva", "12345678900", "(11) 98765-4321", "joao@email.com", "Rua Exemplo, 123", "1990-01-15", "Cliente VIP"};
            for (int i = 0; i < exampleData.length; i++) {
                Cell cell = example.createCell(i);
                cell.setCellValue(exampleData[i]);
                cell.setCellStyle(exampleStyle);
            }
            
            // Adicionar instruções em uma aba separada
            Sheet instructionsSheet = workbook.createSheet("Instruções");
            int rowNum = 0;
            
            Row titleRow = instructionsSheet.createRow(rowNum++);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("📋 INSTRUÇÕES PARA IMPORTAÇÃO DE CLIENTES");
            titleCell.setCellStyle(headerStyle);
            
            rowNum++; // linha em branco
            
            String[] instructions = {
                "1. CAMPOS OBRIGATÓRIOS:",
                "   • name: Nome completo do cliente (mínimo obrigatório: nome OU documento OU email)",
                "",
                "2. FORMATO DOS CAMPOS:",
                "   • name: Texto livre (ex: João da Silva)",
                "   • document: CPF (11 dígitos) ou CNPJ (14 dígitos) sem pontuação",
                "   • phone: Formato livre, recomendado (XX) XXXXX-XXXX",
                "   • email: Email válido (ex: exemplo@email.com)",
                "   • address: Endereço completo",
                "   • birthDate: Formato YYYY-MM-DD (ex: 1990-01-15)",
                "   • observations: Texto livre para observações",
                "",
                "3. DICAS IMPORTANTES:",
                "   • Não altere os nomes das colunas na primeira linha",
                "   • A linha amarela é apenas um exemplo, pode ser removida",
                "   • Linhas completamente vazias serão ignoradas",
                "   • Pelo menos um dos campos (nome, documento ou email) deve estar preenchido",
                "   • Datas devem estar no formato YYYY-MM-DD",
                "",
                "4. APÓS PREENCHER:",
                "   • Salve o arquivo",
                "   • Vá para a tela de Clientes",
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
