package com.gomech.controller;

import com.gomech.entity.Vehicle;
import com.gomech.service.VehicleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vehicles")
public class VehicleController {
    @Autowired
    private VehicleService service;

    @PostMapping
    public ResponseEntity<Vehicle> create(@RequestBody Vehicle client) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.save(client));
    }

    @GetMapping
    public List<Vehicle> list() {
        return service.listAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Vehicle> search(@PathVariable Long id) {
        return service.getById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<Vehicle> update(@PathVariable Long id, @RequestBody Vehicle client) {
        return ResponseEntity.ok(service.update(id, client));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
