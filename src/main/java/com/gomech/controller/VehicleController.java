package com.gomech.controller;

import com.gomech.dto.Vehicles.VehicleCreateDTO;
import com.gomech.dto.Vehicles.VehicleResponseDTO;
import com.gomech.dto.Vehicles.VehicleUpdateDTO;
import com.gomech.dto.PageResponse;
import com.gomech.service.VehicleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.core.io.InputStreamResource;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/vehicles")
public class VehicleController {
    @Autowired
    private VehicleService service;

    @PostMapping
    public ResponseEntity<VehicleResponseDTO> create(@RequestBody VehicleCreateDTO dto) {
        System.out.println(dto);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(service.save(dto));
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<List<VehicleResponseDTO>> upload(@RequestParam("file") MultipartFile file) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(service.saveFromFile(file));
    }

    @GetMapping
    public List<VehicleResponseDTO> list() {
        return service.listAll();
    }

    @GetMapping("/paginated")
    public ResponseEntity<PageResponse<VehicleResponseDTO>> listPaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "ASC") String direction
    ) {
        Sort.Direction sortDirection = direction.equalsIgnoreCase("DESC") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
        Page<VehicleResponseDTO> vehiclePage = service.listAllPaginated(pageable);
        return ResponseEntity.ok(PageResponse.from(vehiclePage));
    }

    @GetMapping("/export")
    public ResponseEntity<InputStreamResource> export(@RequestParam(defaultValue = "csv") String format) {
        var stream = service.exportToFile(format);
        String ext = (format != null && (format.equalsIgnoreCase("xlsx") || format.equalsIgnoreCase("xls"))) ? "xlsx" : "csv";
        MediaType type = ext.equals("xlsx")
                ? MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                : MediaType.parseMediaType("text/csv");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=vehicles." + ext)
                .contentType(type)
                .body(new InputStreamResource(stream));
    }

    @GetMapping("/template")
    public ResponseEntity<InputStreamResource> downloadTemplate(@RequestParam(defaultValue = "xlsx") String format) {
        var stream = service.generateTemplate(format);
        String ext = (format != null && (format.equalsIgnoreCase("xlsx") || format.equalsIgnoreCase("xls"))) ? "xlsx" : "csv";
        MediaType type = ext.equals("xlsx")
                ? MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                : MediaType.parseMediaType("text/csv");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=template_veiculos." + ext)
                .contentType(type)
                .body(new InputStreamResource(stream));
    }

    @GetMapping("/{id}")
    public ResponseEntity<VehicleResponseDTO> search(@PathVariable Long id) {
        return service.getById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<VehicleResponseDTO> update(@PathVariable Long id, @RequestBody VehicleUpdateDTO dto) {
        return ResponseEntity.ok(service.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/service-history")
    public ResponseEntity<List<com.gomech.dto.ServiceOrder.ServiceOrderResponseDTO>> getServiceHistory(@PathVariable Long id) {
        return ResponseEntity.ok(service.getServiceHistory(id));
    }

    @GetMapping("/{id}/service-history/export")
    public ResponseEntity<InputStreamResource> exportServiceHistory(
            @PathVariable Long id,
            @RequestParam(defaultValue = "csv") String format) {
        var stream = service.exportServiceHistory(id, format);
        String ext = (format != null && (format.equalsIgnoreCase("xlsx") || format.equalsIgnoreCase("xls"))) ? "xlsx" : "csv";
        MediaType type = ext.equals("xlsx")
                ? MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                : MediaType.parseMediaType("text/csv");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=vehicle_" + id + "_service_history." + ext)
                .contentType(type)
                .body(new InputStreamResource(stream));
    }
}
