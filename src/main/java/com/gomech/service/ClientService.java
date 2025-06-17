package com.gomech.service;

import com.gomech.model.Client;
import com.gomech.repository.ClientRepository;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Optional;
import java.util.ArrayList;
import java.time.LocalDate;
import java.time.ZoneId;

@Service
public class ClientService {
    @Autowired
    private ClientRepository repository;

    public Client save(Client client) {
        return repository.save(client);
    }

    public List<Client> listAll() {
        return repository.findAll();
    }


    public Optional<Client> getById(Long id) {
        return repository.findById(id);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }

    public Client update(Long id, Client updatedClient) {
        Client client = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Client not found"));
        client.setName(updatedClient.getName());
        client.setDocument(updatedClient.getDocument());
        client.setEmail(updatedClient.getEmail());
        client.setPhone(updatedClient.getPhone());
        return repository.save(client);
    }

    public List<Client> saveAll(List<Client> clients) {
        return repository.saveAll(clients);
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
                Client client = buildClientFromMap(record::get);
                clients.add(client);
            }
        } catch (IOException e) {
            throw new RuntimeException("Falha ao processar arquivo", e);
        }
        return repository.saveAll(clients);
    }

    private List<Client> saveFromExcel(MultipartFile file) {
        List<Client> clients = new ArrayList<>();
        try (InputStream is = file.getInputStream(); Workbook workbook = WorkbookFactory.create(is)) {
            DataFormatter formatter = new DataFormatter();
            Sheet sheet = workbook.getSheetAt(0);
            Map<String, Integer> headers = new HashMap<>();
            boolean first = true;
            for (Row row : sheet) {
                if (first) {
                    for (Cell cell : row) {
                        headers.put(formatter.formatCellValue(cell).toLowerCase(), cell.getColumnIndex());
                    }
                    first = false;
                    continue;
                }
                Map<String, String> rowData = new HashMap<>();
                for (Map.Entry<String, Integer> entry : headers.entrySet()) {
                    Cell cell = row.getCell(entry.getValue());
                    rowData.put(entry.getKey(), formatter.formatCellValue(cell));
                }
                Client client = buildClientFromMap(rowData::get);
                clients.add(client);
            }
        } catch (IOException e) {
            throw new RuntimeException("Falha ao ler planilha", e);
        }
        return repository.saveAll(clients);
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
}
