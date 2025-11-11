package com.gomech.service;

import com.gomech.model.User;
import com.gomech.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class LgpdService {

    private final UserRepository userRepository;
    private final AuditService auditService;

    // Armazenamento temporário de solicitações (Em produção, usar tabela no DB)
    private final Map<String, List<Map<String, Object>>> requestsStorage = new HashMap<>();

    public LgpdService(UserRepository userRepository, AuditService auditService) {
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    /**
     * Obtém status LGPD de um usuário
     */
    public Map<String, Object> getUserLgpdStatus(String userEmail) {
        Map<String, Object> status = new HashMap<>();
        
        try {
            // Verificar se usuário existe
            Optional<User> userOpt = userRepository.findByEmail(userEmail);
            if (userOpt.isEmpty()) {
                status.put("status", "user_not_found");
                status.put("message", "Usuário não encontrado");
                return status;
            }

            // Obter solicitações pendentes do usuário
            List<Map<String, Object>> userRequests = requestsStorage.getOrDefault(userEmail, new ArrayList<>());
            long pendingRequests = userRequests.stream()
                    .filter(req -> "PENDING".equals(req.get("status")))
                    .count();

            // Verificar se há exclusão agendada
            Optional<Map<String, Object>> deletionRequest = userRequests.stream()
                    .filter(req -> "DELETION".equals(req.get("type")) && "PENDING".equals(req.get("status")))
                    .findFirst();

            status.put("status", "active");
            status.put("user_email", userEmail);
            status.put("pending_requests", pendingRequests);
            status.put("total_requests", userRequests.size());
            
            if (deletionRequest.isPresent()) {
                status.put("deletion_scheduled", true);
                status.put("deletion_date", deletionRequest.get().get("scheduled_date"));
            } else {
                status.put("deletion_scheduled", false);
            }

            // Informações sobre direitos LGPD
            Map<String, Object> rights = new HashMap<>();
            rights.put("access", "Você pode solicitar acesso aos seus dados a qualquer momento");
            rights.put("rectification", "Você pode solicitar correção de dados incorretos");
            rights.put("erasure", "Você pode solicitar a exclusão dos seus dados (direito ao esquecimento)");
            rights.put("portability", "Você pode solicitar exportação dos seus dados");
            rights.put("objection", "Você pode se opor ao processamento dos seus dados");
            status.put("rights", rights);

            // Data de última atualização de privacidade
            status.put("last_updated", LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));

            // Registrar consulta de status
            auditService.logEntityAction(
                    "READ",
                    "LGPD_STATUS",
                    null,
                    "User " + userEmail + " consulted their LGPD status"
            );

            return status;

        } catch (Exception e) {
            status.put("status", "error");
            status.put("message", "Erro ao verificar status LGPD: " + e.getMessage());
            return status;
        }
    }

    /**
     * Solicita exclusão de dados (Direito ao esquecimento)
     */
    public Map<String, Object> requestDataDeletion(String userEmail, String reason) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // Verificar se usuário existe
            Optional<User> userOpt = userRepository.findByEmail(userEmail);
            if (userOpt.isEmpty()) {
                result.put("status", "error");
                result.put("message", "Usuário não encontrado");
                return result;
            }

            // Criar solicitação de exclusão
            Map<String, Object> request = new HashMap<>();
            request.put("id", UUID.randomUUID().toString());
            request.put("type", "DELETION");
            request.put("status", "PENDING");
            request.put("reason", reason != null ? reason : "Solicitação do titular dos dados");
            request.put("requested_at", LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));
            request.put("scheduled_date", LocalDateTime.now().plusDays(30).format(DateTimeFormatter.ISO_DATE_TIME));
            
            // Adicionar à lista de solicitações do usuário
            requestsStorage.computeIfAbsent(userEmail, k -> new ArrayList<>()).add(request);

            result.put("status", "success");
            result.put("message", "Solicitação de exclusão registrada com sucesso");
            result.put("request_id", request.get("id"));
            result.put("scheduled_date", request.get("scheduled_date"));
            result.put("note", "Seus dados serão excluídos em 30 dias, conforme legislação LGPD. Você pode cancelar esta solicitação antes desse prazo.");

            // Registrar evento de auditoria
            auditService.logEntityAction(
                    "CREATE",
                    "LGPD_DELETION_REQUEST",
                    null,
                    "User " + userEmail + " requested data deletion. Reason: " + reason
            );

            return result;

        } catch (Exception e) {
            result.put("status", "error");
            result.put("message", "Erro ao processar solicitação: " + e.getMessage());
            return result;
        }
    }

    /**
     * Solicita exportação de dados (Portabilidade)
     */
    public Map<String, Object> requestDataExport(String userEmail) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // Verificar se usuário existe
            Optional<User> userOpt = userRepository.findByEmail(userEmail);
            if (userOpt.isEmpty()) {
                result.put("status", "error");
                result.put("message", "Usuário não encontrado");
                return result;
            }

            // Criar solicitação de exportação
            Map<String, Object> request = new HashMap<>();
            request.put("id", UUID.randomUUID().toString());
            request.put("type", "EXPORT");
            request.put("status", "PENDING");
            request.put("requested_at", LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));
            request.put("estimated_completion", LocalDateTime.now().plusHours(48).format(DateTimeFormatter.ISO_DATE_TIME));
            
            // Adicionar à lista de solicitações do usuário
            requestsStorage.computeIfAbsent(userEmail, k -> new ArrayList<>()).add(request);

            result.put("status", "success");
            result.put("message", "Solicitação de exportação registrada com sucesso");
            result.put("request_id", request.get("id"));
            result.put("estimated_completion", request.get("estimated_completion"));
            result.put("note", "Você receberá um email com o link para download dos seus dados em até 48 horas.");

            // Registrar evento de auditoria
            auditService.logEntityAction(
                    "CREATE",
                    "LGPD_EXPORT_REQUEST",
                    null,
                    "User " + userEmail + " requested data export"
            );

            return result;

        } catch (Exception e) {
            result.put("status", "error");
            result.put("message", "Erro ao processar solicitação: " + e.getMessage());
            return result;
        }
    }

    /**
     * Obtém histórico de solicitações LGPD do usuário
     */
    public Map<String, Object> getUserRequests(String userEmail) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            List<Map<String, Object>> userRequests = requestsStorage.getOrDefault(userEmail, new ArrayList<>());
            
            result.put("status", "success");
            result.put("user_email", userEmail);
            result.put("requests", userRequests);
            result.put("total", userRequests.size());

            return result;

        } catch (Exception e) {
            result.put("status", "error");
            result.put("message", "Erro ao obter solicitações: " + e.getMessage());
            return result;
        }
    }
}

