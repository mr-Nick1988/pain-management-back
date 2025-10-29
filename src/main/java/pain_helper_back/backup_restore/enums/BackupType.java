package pain_helper_back.backup_restore.enums;



/*
 * Типы резервных копий в системе.
 *
 * ТИПЫ:
 * - H2_DATABASE: Бэкап H2 базы данных (основные данные: пациенты, VAS, рекомендации, эскалации)
 * - MONGODB: Бэкап MongoDB (аналитика, отчеты, метрики производительности)
 * - FULL_SYSTEM: Полный системный бэкап (H2 + MongoDB одновременно)
 */
public enum BackupType {
    /*
     * Бэкап H2 базы данных.
     *
     * СОДЕРЖИМОЕ:
     * - Пациенты (Patient)
     * - VAS записи (Vas)
     * - Рекомендации (Recommendation)
     * - Эскалации (Escalation)
     * - Протоколы лечения (TreatmentProtocol)
     * - EMR данные (Emr)
     * - Пользователи (Person)
     *
     * МЕТОД: SQL команда "BACKUP TO" (встроенная в H2)
     * ФОРМАТ: ZIP архив
     */
    H2_DATABASE,

    /*
     * Бэкап MongoDB.
     *
     * СОДЕРЖИМОЕ:
     * - Аналитические события (AnalyticsEvent)
     * - Технические логи (LogEntry)
     * - Отчеты (DailyReportAggregate, WeeklyReportAggregate, MonthlyReportAggregate)
     * - Метрики производительности (PerformanceMetric)
     * - История бэкапов (BackupHistory)
     *
     * МЕТОД: mongodump утилита
     * ФОРМАТ: BSON файлы в директории
     */
    MONGODB,

    /*
     * Полный системный бэкап.
     *
     * СОДЕРЖИМОЕ: H2_DATABASE + MONGODB
     *
     * ИСПОЛЬЗОВАНИЕ: Перед критическими обновлениями системы
     */
    FULL_SYSTEM
}
