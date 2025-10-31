package com.gomech.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class BackupServiceTest {

    @Autowired
    private BackupService backupService;

    private Path lastBackup;

    @AfterEach
    void cleanup() throws IOException {
        if (lastBackup != null && Files.exists(lastBackup)) {
            Files.deleteIfExists(lastBackup);
        }
    }

    @Test
    void backupCreatesFileAndValidatesIntegrity() {
        BackupService.BackupResult result = backupService.performBackup();
        lastBackup = result.file();

        assertThat(Files.exists(result.file())).isTrue();
        assertThat(backupService.verifyIntegrity(result.file(), result.checksum())).isTrue();
    }
}
