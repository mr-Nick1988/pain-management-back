package pain_helper_back.external_emr_integration_service.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import pain_helper_back.external_emr_integration_service.entity.EmrMapping;
import pain_helper_back.enums.EmrSourceType;

import java.util.List;
import java.util.Optional;

/*
 * Репозиторий для работы с маппингом между внешними FHIR ID и внутренними EMR номерами.
 *
 * ЗАЧЕМ НУЖЕН ЭТОТ РЕПОЗИТОРИЙ:
 * - Проверить, был ли пациент уже импортирован из FHIR системы
 * - Найти внутренний EMR номер по внешнему FHIR ID
 * - Найти внешний FHIR ID по внутреннему EMR номеру
 * - Получить список всех импортированных пациентов по источнику
 *
 * ПРИМЕР ИСПОЛЬЗОВАНИЯ:
 * 1. Пациент приходит из больницы А (FHIR ID = "12345")
 * 2. Проверяем: emrMappingRepository.findByExternalFhirId("12345")
 * 3. Если найден → используем существующий EMR номер
 * 4. Если не найден → создаем новый EMR номер и сохраняем маппинг
 */
public interface EmrMappingRepository extends JpaRepository<EmrMapping, Long> {
    /*
     * Найти маппинг по внешнему FHIR ID.
     *
     * КОГДА ИСПОЛЬЗУЕТСЯ:
     * - При импорте пациента из FHIR системы
     * - Чтобы проверить, не импортировали ли мы этого пациента раньше
     *
     * ПРИМЕР:
     * Optional<EmrMapping> mapping = repository.findByExternalFhirId("Patient/12345");
     * if (mapping.isPresent()) {
     *     // Пациент уже импортирован, используем существующий номер
     *     return mapping.get().getInternalEmrNumber();
     * }
     *
     * @param externalFhirId ID пациента в FHIR системе (например, "Patient/12345")
     * @return Optional с маппингом, если найден
     */
    Optional<EmrMapping> findByExternalFhirId(String externalFhirId);
    /*
     * Найти все маппинги по типу источника.
     *
     * КОГДА ИСПОЛЬЗУЕТСЯ:
     * - Для статистики: сколько пациентов импортировано из FHIR vs сколько моковых
     * - Для фильтрации в UI: показать только реальных пациентов или только моковых
     *
     * ПРИМЕР:
     * List<EmrMapping> mockPatients = repository.findBySourceType(EmrSourceType.MOCK_GENERATOR);
     * System.out.println("Моковых пациентов: " + mockPatients.size());
     *
     * @param sourceType тип источника (FHIR_SERVER, MOCK_GENERATOR, EXTERNAL_HOSPITAL)
     * @return список всех маппингов с указанным типом источника
     */
    List<EmrMapping> findBySourceType(EmrSourceType sourceType);
    /*
     * Найти все маппинги по URL источника.
     *
     * КОГДА ИСПОЛЬЗУЕТСЯ:
     * - Чтобы узнать, сколько пациентов импортировано из конкретной больницы
     * - Для аудита: "Показать всех пациентов из больницы Б"
     *
     * ПРИМЕР:
     * List<EmrMapping> patients = repository.findBySourceSystemUrl("https://hospital-b.com/fhir");
     * System.out.println("Пациентов из больницы Б: " + patients.size());
     *
     * @param sourceSystemUrl URL FHIR сервера источника
     * @return список всех маппингов с указанным URL источника
     */
    List<EmrMapping> findBySourceSystemUrl(String sourceSystemUrl);
    /*
     * Проверить, существует ли маппинг для внешнего FHIR ID.
     *
     * КОГДА ИСПОЛЬЗУЕТСЯ:
     * - Быстрая проверка перед импортом: "Уже импортирован?"
     * - Более эффективно, чем findByExternalFhirId (не загружает весь объект)
     *
     * ПРИМЕР:
     * if (repository.existsByExternalFhirId("Patient/12345")) {
     *     throw new AlreadyImportedException("Пациент уже импортирован");
     * }
     *
     * @param externalFhirId ID пациента в FHIR системе
     * @return true если маппинг существует, false иначе
     */
    boolean existsByExternalFhirId(String externalFhirId);

    Optional<EmrMapping> findByInternalEmrNumber(String internalEmrNumber);
}
