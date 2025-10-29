package pain_helper_back.backup_restore.sheduler;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import pain_helper_back.backup_restore.enums.BackupTrigger;
import pain_helper_back.backup_restore.services.BackupManagementService;
import pain_helper_back.backup_restore.services.H2BackupService;
import pain_helper_back.backup_restore.services.MongoBackupService;

@Component
@Slf4j
@RequiredArgsConstructor
public class BackupScheduler {
    private final H2BackupService h2BackupService;
    private final MongoBackupService mongoBackupService;
    private final BackupManagementService backupManagementService;


    @Scheduled(cron = "0 0 2 * * ?") // Ежедневно в 02:00
    public void scheduledH2Backup() {
        log.info("Starting scheduled H2 backup");
        h2BackupService.createBackup(BackupTrigger.SCHEDULED, "SYSTEM");
    }

    @Scheduled(cron = "0 0 3 * * ?") // Ежедневно в 03:00
    public void scheduledMongoBackup() {
        log.info("Starting scheduled MongoDB backup");
        mongoBackupService.createBackup(BackupTrigger.SCHEDULED, "SYSTEM");
    }

    @Scheduled(cron = "0 0 4 * * ?") // Ежедневно в 04:00
    public void scheduledCleanup() {
        log.info("Starting scheduled backup cleanup");
        backupManagementService.cleanupOldBackups();
    }
}
