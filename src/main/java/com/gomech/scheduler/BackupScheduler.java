package com.gomech.scheduler;

import com.gomech.dto.Audit.AuditEventRequest;
import com.gomech.service.AuditService;
import com.gomech.service.BackupService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class BackupScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(BackupScheduler.class);

    private final BackupService backupService;
    private final AuditService auditService;

    public BackupScheduler(BackupService backupService, AuditService auditService) {
        this.backupService = backupService;
        this.auditService = auditService;
    }

    @Scheduled(cron = "${backup.scheduler.cron:0 0 3 * * *}")
    public void executeBackup() {
        LOGGER.info("Iniciando rotina agendada de backup");
        var result = backupService.performBackup();
        boolean integrity = backupService.verifyIntegrity(result.file(), result.checksum());
        String payload = "backupFile=" + result.file().getFileName() + ", checksum=" + result.checksum() + ", integrity=" + integrity;
        AuditEventRequest request = new AuditEventRequest(
                "BACKUP_EXECUTED",
                "Rotina de backup concluída",
                "system@gomech",
                "infraestrutura",
                "SYSTEM",
                java.time.LocalDateTime.now(),
                payload,
                null
        );
        auditService.registerEvent(request);
    }
}
