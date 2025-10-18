package pain_helper_back.external_emr_integration_service.client;


/*
 * Клиент для работы с HAPI FHIR сервером.
 *
 * ЧТО ДЕЛАЕТ ЭТОТ КЛАСС:
 * 1. Получает данные пациентов из FHIR сервера по ID
 * 2. Ищет пациентов по имени и дате рождения
 * 3. Получает лабораторные показатели (Observations) для пациента
 * 4. Конвертирует FHIR ресурсы (Patient, Observation) в наши DTO
 *
 * ТЕХНОЛОГИИ:
 * - HAPI FHIR Client API - для выполнения FHIR запросов
 * - Lombok @Slf4j - для логирования
 * - Lombok @RequiredArgsConstructor - для dependency injection
 *
 * ВАЖНО: Этот класс НЕ работает напрямую с базой данных.
 * Он только получает данные из FHIR сервера и конвертирует их в DTO.
 * Сохранение в БД будет в EmrIntegrationService.
 */

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.*;
import org.springframework.stereotype.Component;
import pain_helper_back.enums.EmrSourceType;
import pain_helper_back.external_emr_integration_service.dto.FhirIdentifierDTO;
import pain_helper_back.external_emr_integration_service.dto.FhirObservationDTO;
import pain_helper_back.external_emr_integration_service.dto.FhirPatientDTO;
import pain_helper_back.external_emr_integration_service.service.IcdCodeLoaderService;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j//автоматическое создание логгера для класса
public class HapiFhirClient {
    /*
     * FHIR клиент, внедряется Spring из FhirConfig.
     *
     * IGenericClient - это интерфейс HAPI FHIR для выполнения операций:
     * - read() - получить ресурс по ID
     * - search() - искать ресурсы по критериям
     * - create() - создать новый ресурс
     * - update() - обновить существующий ресурс
     */
    private final IGenericClient fhirClient;

    /*
     * Получить пациента из FHIR сервера по ID.
     *
     * ЧТО ПРОИСХОДИТ:
     * 1. Выполняется FHIR read операция: GET /Patient/{id}
     * 2. FHIR сервер возвращает Patient ресурс в формате JSON
     * 3. HAPI FHIR автоматически парсит JSON в объект Patient
     * 4. Мы конвертируем Patient в наш FhirPatientDTO
     *
     * ПРИМЕР FHIR ЗАПРОСА:
     * GET http://hapi.fhir.org/baseR4/Patient/123456
     *
     * ПРИМЕР ОТВЕТА (упрощенно):
     * {
     *   "resourceType": "Patient",
     *   "id": "123456",
     *   "name": [{"family": "Smith", "given": ["John"]}],
     *   "birthDate": "1980-01-01",
     *   "gender": "male"
     * } @param fhirPatientId ID пациента в FHIR системе
     * @return DTO с данными пациента
     * @throws ResourceNotFoundException если пациент не найден
     * @throws RuntimeException если произошла ошибка при запросе
     */
    public FhirPatientDTO getPatientById(String fhirPatientId) {
        log.info("Fetching patient from FHIR server: {}", fhirPatientId);

        try {
            // Выполняем FHIR read операцию
            // .read() - начинаем read операцию
            // .resource(Patient.class) - указываем тип ресурса (Patient)
            // .withId(fhirPatientId) - указываем ID пациента
            // .execute() - выполняем запрос
            Patient fhirPatient = fhirClient.read()
                    .resource(Patient.class)
                    .withId(fhirPatientId)
                    .execute();

            log.info("Successfully fetched patient: {} {}",
                    fhirPatient.getNameFirstRep().getGiven().get(0).getValue(),
                    fhirPatient.getNameFirstRep().getFamily());

            // Конвертируем FHIR Patient в наш DTO
            return convertFhirPatientToDto(fhirPatient);
        } catch (ResourceNotFoundException e) {
            //Пациент не найден на FHIR сервере
            log.error("Patient not found in FHIR server: {}", fhirPatientId);
            throw e;
        } catch (Exception e) {
            // Другие ошибки (сеть, таймаут, неправильный формат и т.д.)
            log.error("Error fetching patient from FHIR server: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to fetch patient from FHIR server", e);
        }
    }

    /*
     * Поиск пациентов по имени и дате рождения.
     *
     * ЧТО ПРОИСХОДИТ:
     * 1. Выполняется FHIR search операция с параметрами
     * 2. FHIR сервер возвращает Bundle (набор) пациентов
     * 3. Мы извлекаем Patient ресурсы из Bundle
     * 4. Конвертируем каждого Patient в FhirPatientDTO
     *
     * ПРИМЕР FHIR ЗАПРОСА:
     * GET http://hapi.fhir.org/baseR4/Patient?given=John&family=Smith&birthdate=1980-01-01
     *
     * ЗАЧЕМ НУЖЕН ПОИСК:
     * - Для Patient Reconciliation (сопоставление с существующими пациентами)
     * - Чтобы избежать дубликатов при импорте
     * - Для проверки существует ли пациент в FHIR системе
     *
     * @param firstName имя пациента
     * @param lastName фамилия пациента
     * @param birthDate дата рождения
     * @return список найденных пациентов (может быть пустым)
     */
    public List<FhirPatientDTO> searchPatients(String firstName, String lastName, String birthDate) {
        log.info("Searching patients in FHIR server: {} {} {}", firstName, lastName, birthDate);

        // Выполняем FHIR search операцию
        // .search() - начинаем search операцию
        // .forResource(Patient.class) - ищем Patient ресурсы
        // .where() - добавляем условия поиска
        // Patient.GIVEN - поиск по имени (given name)
        // Patient.FAMILY - поиск по фамилии (family name)
        // Patient.BIRTHDATE - поиск по дате рождения
        // .returnBundle(Bundle.class) - возвращаем результаты как Bundle
        try {
            Bundle bundle = fhirClient.search()
                    .forResource(Patient.class)
                    .where(Patient.GIVEN.matches().value(firstName))
                    .and(Patient.FAMILY.matches().value(lastName))
                    .and(Patient.BIRTHDATE.exactly().day(birthDate))
                    .returnBundle(Bundle.class)
                    .execute();

            List<FhirPatientDTO> patients = new ArrayList<>();

            // Bundle содержит список entry (записей)
            // Каждая entry содержит один ресурс (в нашем случае Patient)
            for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
                if (entry.getResource() instanceof Patient) {
                    Patient fhirPatient = (Patient) entry.getResource();
                    patients.add(convertFhirPatientToDto(fhirPatient));
                }
            }
            log.info("Found {} patients in FHIR server", patients.size());
            return patients;
        } catch (Exception e) {
            log.error("Error searching patients in FHIR server: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to search patients in FHIR server", e);
        }
        /*
         * Получить лабораторные показатели (Observations) для пациента.
         *
         * ЧТО ТАКОЕ OBSERVATION В FHIR:
         * - Лабораторные анализы (кровь, моча)
         * - Витальные показатели (давление, пульс, сатурация)
         * - Измерения (вес, рост, температура)
         *
         * ДЛЯ PMA КРИТИЧНЫ:
         * - GFR (функция почек) - для корректировки дозы препаратов
         * - PLT (тромбоциты) - для оценки риска кровотечений
         * - WBC (лейкоциты) - для оценки инфекций
         * - Sodium (натрий) - для водно-электролитного баланса
         * - SpO2 (сатурация) - для оценки дыхательной функции
         *
         * ПРИМЕР FHIR ЗАПРОСА:
         * GET http://hapi.fhir.org/baseR4/Observation?patient=123456&category=laboratory&_sort=-date&_count=50
         *
         * @param fhirPatientId ID пациента в FHIR системе
         * @return список лабораторных показателей (может быть пустым)
         */
    }

    public List<FhirObservationDTO> getObservationsForPatient(String fhirPatientId) {
        log.info("Fetching observations for patient: {}", fhirPatientId);
        // Выполняем FHIR search для Observation ресурсов
        // .where(Observation.PATIENT.hasId(fhirPatientId)) - только для этого пациента
        // .where(Observation.CATEGORY.exactly().code("laboratory")) - только лабораторные
        // .sort().descending(Observation.DATE) - сортировка по дате (новые первыми)
        // .count(50) - максимум 50 результатов (чтобы не перегружать систему)

        try {
            Bundle bundle = fhirClient.search()
                    .forResource(Observation.class)
                    .where(Observation.PATIENT.hasId(fhirPatientId))
                    .where(Observation.CATEGORY.exactly().code("laboratory"))
                    .sort().descending(Observation.DATE)
                    .count(50)
                    .returnBundle(Bundle.class)
                    .execute();

            List<FhirObservationDTO> observations = new ArrayList<>();

            // Извлекаем Observation ресурсы из Bundle

            for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
                if (entry.getResource() instanceof Observation) {
                    Observation fhirObservation = (Observation) entry.getResource();
                    observations.add(convertFhirObservationToDto(fhirObservation));
                }
            }
            log.info("Found {} observations for patient {}", observations.size(), fhirPatientId);
            return observations;
        } catch (Exception e) {
            log.error("Error fetching observations from FHIR server: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to fetch observations from FHIR server", e);
        }
    }

    /*
     * Конвертирует FHIR Patient ресурс в наш FhirPatientDTO.
     *
     * ЗАЧЕМ НУЖНА КОНВЕРТАЦИЯ:
     * - FHIR Patient - сложный объект с множеством полей
     * - FhirPatientDTO - упрощенная версия только с нужными для PMA полями
     * - Изолирует наш код от изменений в FHIR стандарте
     *
     * ЧТО ИЗВЛЕКАЕМ ИЗ FHIR PATIENT:
     * - ID ресурса
     * - Имя и фамилию (из name[0])
     * - Дату рождения
     * - Пол
     * - Идентификаторы (MRN, страховой полис)
     * - Контакты (телефон, email, адрес)
     *
     * @param fhirPatient FHIR Patient ресурс
     * @return наш DTO с данными пациента
     */
    private FhirPatientDTO convertFhirPatientToDto(Patient fhirPatient) {
        FhirPatientDTO dto = new FhirPatientDTO();
        // ID ресурса в FHIR системе
        dto.setPatientIdInFhirResource(fhirPatient.getIdElement().getIdPart());
        // Имя и фамилия
        // getNameFirstRep() - получить первое имя из списка (обычно основное)
        // getGiven() - список имен (может быть несколько: John Michael)
        // getFamily() - фамилия
        if (fhirPatient.hasName() && !fhirPatient.getName().isEmpty()) {
            HumanName name = fhirPatient.getNameFirstRep();
            if (name.hasGiven()) {
                dto.setFirstName(name.getGiven().get(0).getValue());
            }
            if (name.hasFamily()) {
                dto.setLastName(name.getFamily());
            }
        }
        //Дата рождения
        if (fhirPatient.hasBirthDate()) {
            dto.setDateOfBirth(fhirPatient.getBirthDate().toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate());
        }
        // Пол (male, female, other, unknown)
        if (fhirPatient.hasGender()) {
            dto.setGender(fhirPatient.getGender().toCode());
        }

        //Идентификаторы (MRN, страховой полис и т.д.)
        List<FhirIdentifierDTO> identifiers = new ArrayList<>();
        for (Identifier identifier : fhirPatient.getIdentifier()) {
            FhirIdentifierDTO identifierDTO = new FhirIdentifierDTO();

            if (identifier.hasType() && identifier.getType().hasCoding()) {
                identifierDTO.setType(identifier.getType().getCodingFirstRep().getCode());
            }
            if (identifier.hasSystem()) {
                identifierDTO.setSystem(identifier.getSystem());
            }
            if (identifier.hasValue()) {
                identifierDTO.setValue(identifier.getValue());
            }
            if (identifier.hasUse()) {
                identifierDTO.setUse(identifier.getUse().toCode());
            }
            identifiers.add(identifierDTO);
        }
        dto.setIdentifiers(identifiers);

        // Контактная информация (телефон, email)
        if (fhirPatient.hasTelecom()) {
            for (ContactPoint telecom : fhirPatient.getTelecom()) {
                if (telecom.getSystem() == ContactPoint.ContactPointSystem.PHONE) {
                    dto.setPhoneNumber(telecom.getValue());
                } else if (telecom.getSystem() == ContactPoint.ContactPointSystem.EMAIL) {
                    dto.setEmail(telecom.getValue());
                }
            }
        }
        // Адрес
        if (fhirPatient.hasAddress() && !fhirPatient.getAddress().isEmpty()) {
            Address address = fhirPatient.getAddressFirstRep();
            dto.setAddress(address.getText());
        }
        // Источник данных
        dto.setSourceType(EmrSourceType.FHIR_SERVER);
        dto.setSourceSystemUrl(fhirClient.getServerBase());
        return dto;
    }

    /*
     * Конвертирует FHIR Observation ресурс в наш FhirObservationDTO.
     *
     * ЧТО ИЗВЛЕКАЕМ ИЗ FHIR OBSERVATION:
     * - LOINC код (стандартный код для лабораторных показателей)
     * - Название показателя (человекочитаемое)
     * - Значение (числовое)
     * - Единицу измерения (mg/dL, mmol/L и т.д.)
     * - Дату измерения
     * - Референсный диапазон (нормальные значения)
     * - Интерпретацию (normal, high, low, critical)
     *
     * ПРИМЕР:
     * LOINC 777-3 = Platelets (тромбоциты)
     * Значение = 150
     * Единица = 10*3/uL
     * Референс = 150-400
     * Интерпретация = normal
     *
     * @param fhirObservation FHIR Observation ресурс
     * @return наш DTO с лабораторным показателем
     */
    private FhirObservationDTO convertFhirObservationToDto(Observation fhirObservation) {
        FhirObservationDTO dto = new FhirObservationDTO();
        // ID ресурса
        dto.setFhirObservationInResourceId(fhirObservation.getIdElement().getIdPart());
        // LOINC код и название
        if (fhirObservation.hasCode() && fhirObservation.getCode().hasCoding()) {
            Coding coding = fhirObservation.getCode().getCodingFirstRep();
            dto.setLoincCode(coding.getCode());
            dto.setDisplayName(coding.getDisplay());
        }
        // Значение (числовое)
        if (fhirObservation.hasValueQuantity()) {
            Quantity value = fhirObservation.getValueQuantity();
            dto.setValue(value.getValue().doubleValue());
            dto.setUnit(value.getUnit());
        }
        // Дата измерения
        if (fhirObservation.hasEffectiveDateTimeType()) {
            dto.setEffectiveDateTime(LocalDateTime.ofInstant(
                    fhirObservation.getEffectiveDateTimeType().getValue().toInstant(),
                    ZoneId.systemDefault()));
        } else if (fhirObservation.hasEffectiveInstantType()) {
            dto.setEffectiveDateTime(LocalDateTime.ofInstant(
                    fhirObservation.getEffectiveInstantType().getValue().toInstant(),
                    ZoneId.systemDefault()));
        }
        // Статус (final, preliminary, amended)
        if (fhirObservation.hasStatus()) {
            dto.setStatus(fhirObservation.getStatus().toCode());
        }
        // Референсный диапазон (нормальные значения)
        if (fhirObservation.hasReferenceRange() && !fhirObservation.getReferenceRange().isEmpty()) {
            Observation.ObservationReferenceRangeComponent range = fhirObservation.getReferenceRangeFirstRep();
            if (range.hasLow()) {
                dto.setReferenceRangeLow(range.getLow().getValue().doubleValue());
            }
            if (range.hasHigh()) {
                dto.setReferenceRangeHigh(range.getHigh().getValue().doubleValue());
            }
        }
        // Интерпретация (normal, high, low, critical)
        if (fhirObservation.hasInterpretation() && !fhirObservation.getInterpretation().isEmpty()) {
            dto.setInterpretationOfResult(fhirObservation.getInterpretationFirstRep().getText());
        }
        // ID пациента
        if (fhirObservation.hasSubject()) {
            dto.setPatientReference(fhirObservation.getSubject().getReference());
        }
        return dto;
    }

    /*
     * Получить диагнозы (Conditions) пациента из FHIR сервера.
     *
     * ЧТО ДЕЛАЕТ:
     * 1. Выполняет FHIR запрос: GET /Condition?patient=Patient/123
     * 2. Получает список диагнозов (Condition resources)
     * 3. Извлекает ICD-9 коды и описания
     *
     * ЗАЧЕМ: Диагнозы нужны для:
     * - Выбора правильного протокола лечения
     * - Учета противопоказаний
     * - Аналитики (какие диагнозы чаще приводят к эскалациям)
     *
     * @param patientId FHIR ID пациента
     * @return список ICD кодов с описаниями
     */
    public List<IcdCodeLoaderService.IcdCode> getConditionsForPatient(String patientId) {
        log.debug("Fetching conditions (diagnoses) for patient: {}", patientId);
        
        List<IcdCodeLoaderService.IcdCode> diagnoses = new ArrayList<>();
        
        try {
            // Выполняем FHIR запрос для получения диагнозов
            Bundle conditionBundle = fhirClient.search()
                    .forResource(Condition.class)
                    .where(Condition.PATIENT.hasId(patientId))
                    .returnBundle(Bundle.class)
                    .execute();
            
            if (conditionBundle.hasEntry()) {
                for (Bundle.BundleEntryComponent entry : conditionBundle.getEntry()) {
                    if (entry.getResource() instanceof Condition) {
                        Condition condition = (Condition) entry.getResource();
                        
                        // Извлекаем ICD код и описание
                        if (condition.hasCode() && condition.getCode().hasCoding()) {
                            for (Coding coding : condition.getCode().getCoding()) {
                                // Ищем ICD-9 коды (система: http://hl7.org/fhir/sid/icd-9-cm)
                                if (coding.hasSystem() && coding.getSystem().contains("icd-9")) {
                                    String code = coding.getCode();
                                    String description = coding.hasDisplay() ? coding.getDisplay() : 
                                                        condition.getCode().hasText() ? condition.getCode().getText() : "Unknown";
                                    
                                    diagnoses.add(new pain_helper_back.external_emr_integration_service.service.IcdCodeLoaderService.IcdCode(code, description));
                                    log.debug("Found ICD-9 diagnosis: {} - {}", code, description);
                                }
                            }
                        }
                    }
                }
                log.info("Retrieved {} diagnoses for patient: {}", diagnoses.size(), patientId);
            } else {
                log.warn("No conditions found for patient: {}", patientId);
            }
            
        } catch (Exception e) {
            log.error("Error fetching conditions for patient {}: {}", patientId, e.getMessage());
        }
        
        return diagnoses;
    }
}
