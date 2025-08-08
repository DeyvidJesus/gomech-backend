package com.gomech.controller;

import com.gomech.model.Client;
import com.gomech.service.ClientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.core.io.InputStreamResource;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/clients")
public class ClientController {
    @Autowired
    private ClientService service;

    @PostMapping
    public ResponseEntity<Client> create(@RequestBody Client client) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.save(client));
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<List<Client>> upload(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.saveFromFile(file));
    }

    @GetMapping
    public List<Client> list() {
        return service.listAll();
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

    @GetMapping("/{id}")
    public ResponseEntity<Client> search(@PathVariable Long id) {
        return service.getById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<Client> update(@PathVariable Long id, @RequestBody Client client) {
        return ResponseEntity.ok(service.update(id, client));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
