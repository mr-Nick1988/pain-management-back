package pain_helper_back.backup_restore.controller;


import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pain_helper_back.backup_restore.dto.BackupRequestDTO;
import pain_helper_back.backup_restore.dto.BackupResponseDTO;
import pain_helper_back.backup_restore.dto.BackupStatisticsDTO;
import pain_helper_back.backup_restore.dto.RestoreRequestDTO;
import pain_helper_back.backup_restore.services.BackupManagementService;

import java.util.List;

@RestController
@RequestMapping("/api/backup")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class BackupController {
    private final BackupManagementService backupManagementService;

    @PostMapping("/create")
    public ResponseEntity<BackupResponseDTO> createBackup(@Valid @RequestBody BackupRequestDTO request) {
        log.info("POST /api/backup/create - type: {}", request.getBackupType());
        return ResponseEntity.ok(backupManagementService.createBackup(request));
    }

    @PostMapping("/restore")
    public ResponseEntity<String> restoreBackup(@Valid @RequestBody RestoreRequestDTO request) {
        log.info("POST /api/backup/restore - backupId: {}", request.getBackupId());
        return ResponseEntity.ok(backupManagementService.restoreFromBackup(request));
    }

    @GetMapping("/history")
    public ResponseEntity<List<BackupResponseDTO>> getHistory() {
        return ResponseEntity.ok(backupManagementService.getBackupHistory());
    }

    @GetMapping("/statistics")
    public ResponseEntity<BackupStatisticsDTO> getStatistics() {
        return ResponseEntity.ok(backupManagementService.getStatistics());
    }

    @DeleteMapping("/cleanup")
    public ResponseEntity<String> cleanup() {
        int deleted = backupManagementService.cleanupOldBackups();
        return ResponseEntity.ok("Deleted " + deleted + " old backups");
    }
}
