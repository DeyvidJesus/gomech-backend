package com.gomech.service;

import com.gomech.dto.Vehicles.VehicleCreateDTO;
import com.gomech.dto.Vehicles.VehicleResponseDTO;
import com.gomech.dto.Vehicles.VehicleUpdateDTO;
import com.gomech.model.Client;
import com.gomech.model.Vehicle;
import com.gomech.repository.ClientRepository;
import com.gomech.repository.VehicleRepository;
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

    public Optional<VehicleResponseDTO> getById(Long id) {
        return repository.findById(id)
                .map(VehicleResponseDTO::fromEntity);
    }

    public void delete(Long id) {
        repository.deleteById(id);
        auditService.logEntityAction("DELETE", "VEHICLE", id,
                "Veículo removido");
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
                Vehicle vehicle = buildVehicleFromMap(rowData::get);
                vehicles.add(vehicle);
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
                CSVFormat.DEFAULT.withHeader("licensePlate", "brand", "model", "manufactureDate", "color", "observations", "kilometers", "chassisId"))) {
            for (Vehicle v : vehicles) {
                String manufactureDate = v.getManufactureDate() != null ?
                        LocalDate.ofInstant(v.getManufactureDate().toInstant(), ZoneId.systemDefault()).toString() : "";
                printer.printRecord(v.getLicensePlate(), v.getBrand(), v.getModel(), manufactureDate, v.getColor(), v.getObservations(), v.getKilometers(), v.getChassisId());
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
            String[] headers = {"licensePlate", "brand", "model", "manufactureDate", "color", "observations", "kilometers", "chassisId"};
            Row header = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                header.createCell(i).setCellValue(headers[i]);
            }
            int rowIdx = 1;
            for (Vehicle v : vehicles) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(v.getLicensePlate());
                row.createCell(1).setCellValue(v.getBrand());
                row.createCell(2).setCellValue(v.getModel());
                String manufactureDate = v.getManufactureDate() != null ?
                        LocalDate.ofInstant(v.getManufactureDate().toInstant(), ZoneId.systemDefault()).toString() : "";
                row.createCell(3).setCellValue(manufactureDate);
                row.createCell(4).setCellValue(v.getColor());
                row.createCell(5).setCellValue(v.getObservations());
                row.createCell(6).setCellValue(v.getKilometers());
                row.createCell(7).setCellValue(v.getChassisId());
            }
            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException("Falha ao gerar planilha", e);
        }
    }
}
