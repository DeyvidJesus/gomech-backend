package com.gomech.controller;

import com.gomech.service.LgpdService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/lgpd")
public class LgpdController {

    private final LgpdService lgpdService;

    public LgpdController(LgpdService lgpdService) {
        this.lgpdService = lgpdService;
    }

    /**
     * Verifica status LGPD de um usuário
     * 
     * @param userEmail Email do usuário
     * @return Status com solicitações pendentes, exclusões agendadas, etc.
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus(
            @RequestParam(value = "userEmail", required = true) String userEmail) {
        Map<String, Object> status = lgpdService.getUserLgpdStatus(userEmail);
        return ResponseEntity.ok(status);
    }

    /**
     * Solicita exclusão de dados (Direito ao esquecimento)
     * 
     * @param userEmail Email do usuário solicitante
     * @return Confirmação da solicitação
     */
    @PostMapping("/request-deletion")
    public ResponseEntity<Map<String, Object>> requestDeletion(
            @RequestParam(value = "userEmail", required = true) String userEmail,
            @RequestParam(value = "reason", required = false) String reason) {
        Map<String, Object> result = lgpdService.requestDataDeletion(userEmail, reason);
        return ResponseEntity.ok(result);
    }

    /**
     * Solicita exportação de dados (Portabilidade)
     * 
     * @param userEmail Email do usuário solicitante
     * @return Confirmação da solicitação
     */
    @PostMapping("/request-export")
    public ResponseEntity<Map<String, Object>> requestExport(
            @RequestParam(value = "userEmail", required = true) String userEmail) {
        Map<String, Object> result = lgpdService.requestDataExport(userEmail);
        return ResponseEntity.ok(result);
    }

    /**
     * Consulta histórico de solicitações LGPD
     * 
     * @param userEmail Email do usuário
     * @return Lista de solicitações
     */
    @GetMapping("/requests")
    public ResponseEntity<Map<String, Object>> getRequests(
            @RequestParam(value = "userEmail", required = true) String userEmail) {
        Map<String, Object> requests = lgpdService.getUserRequests(userEmail);
        return ResponseEntity.ok(requests);
    }
}

