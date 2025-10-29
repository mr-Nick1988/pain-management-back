package pain_helper_back.backup_restore.repository;


import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import pain_helper_back.backup_restore.entity.BackupHistory;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository для работы с историей бэкапов в MongoDB.
 *
 * КОЛЛЕКЦИЯ: backup_history
 *
 * ИНДЕКСЫ:
 * - backupType (для фильтрации по типу)
 * - status (для поиска успешных/неудачных)
 * - startTime (для сортировки по времени)
 * - expirationDate (для автоматической очистки)
 *
 * МЕТОДЫ:
 * - Поиск по типу, статусу, периоду
 * - Поиск истекших бэкапов для очистки
 * - Получение последних бэкапов
 */
@Repository
public interface BackupHistoryRepository extends MongoRepository<BackupHistory, String> {

    /*
     * Найти все бэкапы определенного типа.
     *
     * @param backupType Тип бэкапа (H2_DATABASE, MONGODB, FULL_SYSTEM)
     * @return Список бэкапов
     */
    List<BackupHistory> findByBackupType(String backupType);

    /*
     * Найти все бэкапы с определенным статусом.
     *
     * @param status Статус (IN_PROGRESS, SUCCESS, FAILED)
     * @return Список бэкапов
     */
    List<BackupHistory> findByStatus(String status);

    /*
     * Найти бэкапы по типу и статусу.
     *
     * ИСПОЛЬЗОВАНИЕ: Получить все успешные бэкапы H2 для восстановления
     *
     * @param backupType Тип бэкапа
     * @param status Статус
     * @return Список бэкапов
     */
    List<BackupHistory> findByBackupTypeAndStatus(String backupType, String status);

    /*
     * Найти бэкапы за определенный период.
     *
     * @param startDate Начало периода
     * @param endDate Конец периода
     * @return Список бэкапов
     */
    List<BackupHistory> findByStartTimeBetween(LocalDateTime startDate, LocalDateTime endDate);

    /*
     * Найти бэкапы с истекшим сроком хранения.
     *
     * ИСПОЛЬЗОВАНИЕ: Автоматическая очистка старых бэкапов (политика 30 дней)
     *
     * @param date Текущая дата
     * @return Список истекших бэкапов
     */
    List<BackupHistory> findByExpirationDateBefore(LocalDateTime date);

    /*
     * Найти последние бэкапы (сортировка по времени DESC).
     *
     * @param pageable Параметры пагинации (размер страницы)
     * @return Список последних бэкапов
     */
    List<BackupHistory> findByOrderByStartTimeDesc(Pageable pageable);

    /*
     * Найти последние успешные бэкапы определенного типа.
     *
     * ИСПОЛЬЗОВАНИЕ: Получить последний успешный бэкап для восстановления
     *
     * @param backupType Тип бэкапа
     * @param status Статус (обычно SUCCESS)
     * @param pageable Параметры пагинации
     * @return Список бэкапов
     */
    List<BackupHistory> findByBackupTypeAndStatusOrderByStartTimeDesc(
            String backupType, String status, Pageable pageable);
}