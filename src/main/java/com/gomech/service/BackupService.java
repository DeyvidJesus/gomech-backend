package com.gomech.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;

@Service
public class BackupService {

    private final Path backupDirectory;
    private final EncryptionService encryptionService;

    public record BackupResult(Path file, String checksum, Instant timestamp) {}

    public BackupService(@Value("${backup.directory:backups}") String directory,
                         EncryptionService encryptionService) {
        this.backupDirectory = Path.of(directory);
        this.encryptionService = encryptionService;
    }

    public BackupResult performBackup() {
        try {
            Files.createDirectories(backupDirectory);
            Instant timestamp = Instant.now();
            String content = "GoMech backup :: " + timestamp;
            Path backupFile = backupDirectory.resolve("backup-" + timestamp.toEpochMilli() + ".bak");
            Files.writeString(backupFile, content, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
            String checksum = encryptionService.sha256(content);
            return new BackupResult(backupFile, checksum, timestamp);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to create backup", e);
        }
    }

    public boolean verifyIntegrity(Path file, String expectedChecksum) {
        try {
            if (!Files.exists(file)) {
                return false;
            }
            String content = Files.readString(file);
            String checksum = encryptionService.sha256(content);
            return checksum.equals(expectedChecksum);
        } catch (IOException e) {
            return false;
        }
    }
}
