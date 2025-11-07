package com.gomech.controller;

import com.gomech.dto.Clients.ClientCreateDTO;
import com.gomech.dto.Clients.ClientResponseDTO;
import com.gomech.dto.Clients.ClientUpdateDTO;
import com.gomech.dto.PageResponse;
import com.gomech.model.Client;
import com.gomech.service.ClientService;
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
import java.util.Optional;

@RestController
@RequestMapping("/clients")
public class ClientController {
    @Autowired
    private ClientService service;

    @PostMapping
    public ResponseEntity<ClientResponseDTO> create(@RequestBody ClientCreateDTO dto) {
        Client client = service.save(dto);
        return ResponseEntity.ok(ClientResponseDTO.fromEntity(client));
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<List<Client>> upload(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.saveFromFile(file));
    }

    @GetMapping
    public ResponseEntity<List<ClientResponseDTO>> list() {
        List<ClientResponseDTO> clients = service.listAll()
                .stream()
                .map(ClientResponseDTO::fromEntity)
                .toList();
        return ResponseEntity.ok(clients);
    }

    @GetMapping("/paginated")
    public ResponseEntity<PageResponse<ClientResponseDTO>> listPaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "ASC") String direction
    ) {
        Sort.Direction sortDirection = direction.equalsIgnoreCase("DESC") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
        Page<Client> clientPage = service.listAllPaginated(pageable);
        Page<ClientResponseDTO> responsePage = clientPage.map(ClientResponseDTO::fromEntity);
        return ResponseEntity.ok(PageResponse.from(responsePage));
    }

    @GetMapping("/export")
    public ResponseEntity<InputStreamResource> export(@RequestParam(defaultValue = "csv") String format) {
        var stream = service.exportToFile(format);
        String ext = (format != null && (format.equalsIgnoreCase("xlsx") || format.equalsIgnoreCase("xls"))) ? "xlsx" : "csv";
        MediaType type = ext.equals("xlsx") ? MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet") : MediaType.parseMediaType("text/csv");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=clients." + ext)
                .contentType(type)
                .body(new InputStreamResource(stream));
    }

    @GetMapping("/template")
    public ResponseEntity<InputStreamResource> downloadTemplate(@RequestParam(defaultValue = "xlsx") String format) {
        var stream = service.generateTemplate(format);
        String ext = (format != null && (format.equalsIgnoreCase("xlsx") || format.equalsIgnoreCase("xls"))) ? "xlsx" : "csv";
        MediaType type = ext.equals("xlsx") ? MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet") : MediaType.parseMediaType("text/csv");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=template_clientes." + ext)
                .contentType(type)
                .body(new InputStreamResource(stream));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClientResponseDTO> getById(@PathVariable Long id) {
        Client client = service.getById(id).orElseThrow(() -> new RuntimeException("Cliente não encontrado com o ID: " + id));
        return ResponseEntity.ok(ClientResponseDTO.fromEntity(client));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ClientResponseDTO> update(
            @PathVariable Long id,
            @RequestBody ClientUpdateDTO dto
    ) {
        Client client = service.update(id, dto);
        return ResponseEntity.ok(ClientResponseDTO.fromEntity(client));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
