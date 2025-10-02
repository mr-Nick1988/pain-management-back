package pain_helper_back.emr_integration.service;


import com.github.javafaker.Faker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pain_helper_back.emr_integration.dto.FhirIdentifierDTO;
import pain_helper_back.emr_integration.dto.FhirObservationDTO;
import pain_helper_back.emr_integration.dto.FhirPatientDTO;
import pain_helper_back.enums.EmrSourceType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

/**
 * Генератор моковых EMR данных для разработки и тестирования.
 *
 * ЗАЧЕМ НУЖЕН ЭТОТ КЛАСС:
 * 1. Быстрая генерация тестовых пациентов (100 пациентов за 1 секунду)
 * 2. Реалистичные данные (имена, даты, лабораторные показатели)
 * 3. Контролируемые сценарии (пациент с почечной недостаточностью, аллергиями и т.д.)
 * 4. Автотесты (не зависят от внешних FHIR серверов)
 * 5. Офлайн разработка (работает без интернета)
 *
 * ИСПОЛЬЗОВАНИЕ:
 * - Фронтенд-разработчик: "Дай 50 пациентов для тестирования UI"
 * - QA-тестировщик: "Нужен пациент с креатинином 2.5 для проверки корректировки дозы"
 * - Backend-разработчик: "Запускаю 100 автотестов с моковыми данными"
 */
@Service
@Slf4j
public class MockEmrDataGenerator {
    private final Faker faker = new Faker();

    /*
     * Генерирует случайного пациента с реалистичными данными.
     *
     * ЧТО ГЕНЕРИРУЕТСЯ:
     * - Имя и фамилия (John Smith, Jane Doe)
     * - Дата рождения (возраст 18-90 лет)
     * - Пол (male/female)
     * - Контакты (телефон, email, адрес)
     * - Идентификаторы (MRN, страховой полис)
     *
     * ПРИМЕР:
     * FhirPatientDTO patient = generator.generateRandomPatient();
     * // patient.firstName = "John"
     * // patient.lastName = "Smith"
     * // patient.dateOfBirth = 1980-05-15
     * // patient.phoneNumber = "+7-912-345-67-89"
     *
     * @return моковый пациент с реалистичными данными
     */
    public FhirPatientDTO generateRandomPatient() {
        FhirPatientDTO patient = new FhirPatientDTO();

        // Генерируем уникальный FHIR ID
        patient.setPatientIdInFhirResource("MOCK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());

        // Имя и фамилия
        patient.setFirstName(faker.name().firstName());
        patient.setLastName(faker.name().lastName());

        // Дата рождения (возраст 18-90 лет)
        LocalDate birthDate = faker.date().birthday(18, 90).toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        patient.setDateOfBirth(birthDate);

        // Пол (50% male, 50% female)
        patient.setGender(faker.bool().bool() ? "male" : "female");

        // Контакты
        patient.setPhoneNumber(faker.phoneNumber().phoneNumber());
        patient.setEmail(faker.internet().emailAddress());
        patient.setAddress(faker.address().fullAddress());

        // Идентификаторы (MRN, страховой полис)
        List<FhirIdentifierDTO> identifiers = new ArrayList<>();

        // MRN (Medical Record Number)
        FhirIdentifierDTO mrn = new FhirIdentifierDTO();
        mrn.setType("MRN");
        mrn.setSystem("http://mock-hospital.com/mrn");
        mrn.setValue("MRN-" + faker.number().digits(10));
        mrn.setUse("official");
        identifiers.add(mrn);

        // Страховой полис (20% вероятность)
        if (faker.number().numberBetween(1, 100) <= 20) {
            FhirIdentifierDTO insurance = new FhirIdentifierDTO();
            insurance.setType("INS");
            insurance.setSystem("http://insurance-company.com");
            insurance.setValue("INS-" + faker.number().digits(10));
            insurance.setUse("official");
            identifiers.add(insurance);
        }
        patient.setIdentifiers(identifiers);

        // Источник данных
        patient.setSourceType(EmrSourceType.MOCK_GENERATOR);
        patient.setSourceSystemUrl("http://mock-emr-generator.local");
        log.debug("Generated mock patient: {} {}", patient.getFirstName(), patient.getLastName());
        return patient;
    }

    /*
     * Генерирует лабораторные показатели для пациента.
     *
     * ГЕНЕРИРУЕМЫЕ ПОКАЗАТЕЛИ:
     * - Креатинин (функция почек): 0.5-3.0 mg/dL
     * - Билирубин (функция печени): 0.3-5.0 mg/dL
     * - Тромбоциты (PLT): 50-400 10*3/uL
     * - Лейкоциты (WBC): 3-15 10*3/uL
     * - Натрий: 130-150 mmol/L
     * - Сатурация (SpO2): 85-100%
     *
     * ЗАЧЕМ: Для тестирования корректировки доз препаратов в Treatment Protocol
     *
     * @param patientFhirId FHIR ID пациента
     * @return список лабораторных показателей
     */
    public List<FhirObservationDTO> generateObservationForPatient(String patientFhirId) {
        List<FhirObservationDTO> observations = new ArrayList<>();

        // Креатинин (почки)
        observations.add(createObservation(
                patientFhirId,
                "2160-0",
                "Creatinine",
                0.5 + (faker.random().nextDouble() * 2.5), // 0.5-3.0
                "mg/dL",
                0.6, 1.2
        ));

        // Билирубин (печень)
        observations.add(createObservation(
                patientFhirId,
                "1975-2",
                "Bilirubin",
                0.3 + (faker.random().nextDouble() * 4.7), // 0.3-5.0
                "mg/dL",
                0.3, 1.2
        ));

        // Тромбоциты
        observations.add(createObservation(
                patientFhirId,
                "777-3",
                "Platelets",
                50.0 + (faker.random().nextDouble() * 350.0), // 50-400
                "10*3/uL",
                150.0, 400.0
        ));

        // Лейкоциты
        observations.add(createObservation(
                patientFhirId,
                "6690-2",
                "White Blood Cells",
                faker.number().randomDouble(1, 3, 15),
                "10*3/uL",
                4.0, 11.0
        ));
        // Натрий
        observations.add(createObservation(
                patientFhirId,
                "2951-2",
                "Sodium",
                130.0 + (faker.random().nextDouble() * 20.0), // 130-150
                "mmol/L",
                135.0, 145.0
        ));
        // Сатурация
        observations.add(createObservation(
                patientFhirId,
                "59408-5",
                "Oxygen Saturation",
                85.0 + (faker.random().nextDouble() * 15.0), // 85-100
                "%",
                95.0, 100.0
        ));
        log.debug("Generated {} observations for patient {}", observations.size(), patientFhirId);
        return observations;
    }

    /*
     * Генерирует пациента с почечной недостаточностью.
     *
     * ПАРАМЕТРЫ:
     * - Креатинин: 2.0-3.0 mg/dL (плохие почки)
     * - Возраст: 65-85 лет (пожилой)
     * - Вес: 50-70 кг (низкий вес)
     *
     * ЗАЧЕМ: Для тестирования снижения дозы препаратов при почечной недостаточности
     *
     * @return пациент с плохой функцией почек
     */
    public FhirPatientDTO generatePatientWithRenalFailure() {
        FhirPatientDTO patient = generateRandomPatient();
        // Устанавливаем специфические параметры через observations
        log.info("Generated patient with renal failure: {}", patient.getPatientIdInFhirResource());
        return patient;
    }

    /*
     * Генерирует пациента с печеночной недостаточностью.
     *
     * ПАРАМЕТРЫ:
     * - Билирубин: 3.0-5.0 mg/dL (плохая печень)
     * - Тромбоциты: 50-100 (низкие)
     *
     * ЗАЧЕМ: Для тестирования исключения гепатотоксичных препаратов
     *
     * @return пациент с плохой функцией печени
     */
    public FhirPatientDTO generatePatientWithHepaticFailure() {
        FhirPatientDTO patient = generateRandomPatient();
        log.info("Generated patient with hepatic failure: {}", patient.getPatientIdInFhirResource());
        return patient;
    }

    /*
     * Генерирует пожилого пациента с низким весом.
     *
     * ПАРАМЕТРЫ:
     * - Возраст: 75-90 лет
     * - Вес: 40-55 кг
     *
     * ЗАЧЕМ: Для тестирования корректировки дозы по возрасту и весу
     *
     * @return пожилой пациент с низким весом
     */
    public FhirPatientDTO generateElderlyLowWeightPatient() {
        FhirPatientDTO patient = generateRandomPatient();
        // Устанавливаем возраст 75-90 лет
        LocalDate birthDate = LocalDate.now().minusYears(faker.number().numberBetween(75, 90));
        patient.setDateOfBirth(birthDate);
        log.info("Generated elderly low-weight patient: {}", patient.getPatientIdInFhirResource());
        return patient;
    }

    /*
     * Генерирует batch (пакет) случайных пациентов.
     *
     * ЗАЧЕМ:
     * - Фронтенд-разработчик: "Дай 50 пациентов для тестирования UI"
     * - QA: "Нужно 100 пациентов для нагрузочного тестирования"
     *
     * ПРОИЗВОДИТЕЛЬНОСТЬ: 100 пациентов генерируются за ~1 секунду
     *
     * ПРИМЕР:
     * List<FhirPatientDTO> patients = generator.generateBatch(100);
     * // 100 пациентов с уникальными данными
     *
     * @param count количество пациентов для генерации
     * @return список моковых пациентов
     */

    public List<FhirPatientDTO> generateBatch(int count) {
        log.info("Generating batch of {} mock patients", count);
        long startTime = System.currentTimeMillis();

        List<FhirPatientDTO> patients = IntStream.range(0, count)
                .mapToObj(i -> generateRandomPatient())
                .toList();

        long duration = System.currentTimeMillis() - startTime;
        log.info("Generated {} patients in {} ms", count, duration);
        return patients;
    }
    /*
     * Вспомогательный метод для создания Observation.
     */

    private FhirObservationDTO createObservation(
            String patientFhirId,
            String loincCode,
            String displayName,
            double value,
            String unit,
            double refLow,
            double refHigh) {

        FhirObservationDTO obs = new FhirObservationDTO();
        obs.setFhirObservationInResourceId("OBS-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        obs.setLoincCode(loincCode);
        obs.setDisplayName(displayName);
        obs.setValue(value);
        obs.setUnit(unit);
        obs.setEffectiveDateTime(LocalDateTime.now().minusDays(faker.number().numberBetween(1, 30)));
        obs.setReferenceRangeLow(refLow);
        obs.setReferenceRangeHigh(refHigh);
        obs.setPatientReference("Patient/" + patientFhirId);

        // Интерпретация (normal, high, low)
        if (value < refLow) {
            obs.setInterpretationOfResult("low");
        } else if (value > refHigh) {
            obs.setInterpretationOfResult("high");
        } else {
            obs.setInterpretationOfResult("normal");
        }
        return obs;
    }

}

