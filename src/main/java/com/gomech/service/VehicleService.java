package com.gomech.service;

import com.gomech.model.Vehicle;
import com.gomech.repository.VehicleRepository;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

@Service
public class VehicleService {
    @Autowired
    private VehicleRepository repository;

    public Vehicle save(Vehicle vehicle) {
        return repository.save(vehicle);
    }

    public List<Vehicle> listAll() {
        return repository.findAll();
    }


    public Optional<Vehicle> getById(Long id) {
        return repository.findById(id);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }

    public Vehicle update(Long id, Vehicle updatedVehicle) {
        Vehicle vehicle = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Vehicle not found"));
        vehicle.setLicensePlate(updatedVehicle.getLicensePlate());
        vehicle.setBrand(updatedVehicle.getBrand());
        vehicle.setModel(updatedVehicle.getModel());
        vehicle.setColor(updatedVehicle.getColor());
        vehicle.setManufactureDate((updatedVehicle.getManufactureDate()));
        return repository.save(vehicle);
    }

    public List<Vehicle> saveFromFile(MultipartFile file) {
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
        return repository.saveAll(vehicles);
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
        return repository.saveAll(vehicles);
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
                vehicle.setKilometers(Float.parseFloat(km));
            } catch (NumberFormatException ignored) {}
        }
        vehicle.setVehicleId(getter.apply("vehicleId"));
        if (vehicle.getVehicleId() == null) {
            vehicle.setVehicleId(getter.apply("vehicleid"));
        }

        return vehicle;
    }
}
