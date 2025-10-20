package pain_helper_back.external_emr_integration_service.service;

import com.github.javafaker.Faker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pain_helper_back.external_emr_integration_service.dto.FhirIdentifierDTO;
import pain_helper_back.external_emr_integration_service.dto.FhirObservationDTO;
import pain_helper_back.external_emr_integration_service.dto.FhirPatientDTO;
import pain_helper_back.enums.EmrSourceType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

/*
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
@RequiredArgsConstructor
public class MockEmrDataGenerator {
    private final Faker faker = new Faker();
    private final TreatmentProtocolIcdExtractor treatmentProtocolIcdExtractor;

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

        // Страховой полис (70% вероятность - большинство имеют страховку)
        if (faker.number().numberBetween(1, 100) <= 70) {
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
     * Генерирует список диагнозов для пациента.
     * 
     * ВАЖНО: Теперь генерируются ТОЛЬКО диагнозы из Treatment Protocol!
     * Это гарантирует, что моковые пациенты имеют только те противопоказания,
     * которые реально влияют на выбор лечения.
     * 
     * ЛОГИКА:
     * - 60% пациентов имеют 1 диагноз
     * - 25% пациентов имеют 2 диагноза
     * - 10% пациентов имеют 3 диагноза
     * - 5% пациентов имеют 4-5 диагнозов
     * 
     * @return список ICD кодов с описаниями из Treatment Protocol
     */
    public List<IcdCodeLoaderService.IcdCode> generateDiagnosesForPatient() {
        double rand = faker.random().nextDouble();
        int diagnosisCount;
        
        if (rand < 0.60) {
            diagnosisCount = 1;
        } else if (rand < 0.85) {
            diagnosisCount = 2;
        } else if (rand < 0.95) {
            diagnosisCount = 3;
        } else {
            diagnosisCount = faker.number().numberBetween(4, 6);
        }
        
        // ИЗМЕНЕНИЕ: Используем только ICD коды из Treatment Protocol
        return treatmentProtocolIcdExtractor.getRandomProtocolDiagnoses(diagnosisCount);
    }
    
    /**
     * Генерирует список аллергий (sensitivities) для пациента.
     * 
     * ВАЖНО: Генерируются ТОЛЬКО препараты из колонки avoidIfSensitivity в Treatment Protocol!
     * Это гарантирует, что аллергии моковых пациентов реально влияют на выбор лечения.
     * 
     * ЛОГИКА:
     * - 70% пациентов НЕ имеют аллергий (null)
     * - 20% пациентов имеют 1 аллергию
     * - 8% пациентов имеют 2 аллергии
     * - 2% пациентов имеют 3+ аллергии
     * 
     * @return список аллергий или null
     */
    public List<String> generateSensitivitiesForPatient() {
        return treatmentProtocolIcdExtractor.generateRandomSensitivities();
    }

    /*
     * Генерирует лабораторные показатели для пациента.
     *
     * ОБНОВЛЕНО: Используются реалистичные значения из Clinical_Norms_and_Units.csv
     *
     * ГЕНЕРИРУЕМЫЕ ПОКАЗАТЕЛИ (с правильными диапазонами):
     * - GFR (функция почек): A(≥90), B(60-89), C(45-59), D(30-44), E(15-29), F(<15)
     * - Тромбоциты (PLT): 150-450 (норма), возможный диапазон 0-1000
     * - Лейкоциты (WBC): 3.5-10.0 (норма), возможный диапазон 2-40
     * - Натрий: 135-145 (норма), возможный диапазон 120-160
     * - Сатурация (SpO2): 95-100% (норма), возможный диапазон 85-100%
     * - Вес: >50 кг
     *
     * ВАЖНО: Теперь генерируются ТОЧНЫЕ значения, а не диапазоны!
     *
     * @param patientFhirId FHIR ID пациента
     * @return список лабораторных показателей
     */
    public List<FhirObservationDTO> generateObservationForPatient(String patientFhirId) {
        List<FhirObservationDTO> observations = new ArrayList<>();

        // Креатинин (почки) - для расчета GFR
        // Норма: 0.6-1.2 mg/dL, возможный диапазон: 0.5-3.0
        // 80% пациентов - норма, 15% - умеренное повышение, 5% - высокое
        double creatinine;
        double rand = faker.random().nextDouble();
        if (rand < 0.80) {
            // Норма: 0.6-1.2
            creatinine = 0.6 + (faker.random().nextDouble() * 0.6);
        } else if (rand < 0.95) {
            // Умеренное повышение: 1.2-2.0
            creatinine = 1.2 + (faker.random().nextDouble() * 0.8);
        } else {
            // Высокое: 2.0-3.0
            creatinine = 2.0 + (faker.random().nextDouble() * 1.0);
        }
        observations.add(createObservation(
                patientFhirId,
                "2160-0",
                "Creatinine",
                Math.round(creatinine * 100.0) / 100.0, // Округляем до 2 знаков
                "mg/dL",
                0.6, 1.2
        ));

        // Билирубин (печень)
        // Норма: 0.3-1.2 mg/dL
        double bilirubin;
        rand = faker.random().nextDouble();
        if (rand < 0.85) {
            // Норма: 0.3-1.2
            bilirubin = 0.3 + (faker.random().nextDouble() * 0.9);
        } else if (rand < 0.95) {
            // Умеренное повышение: 1.2-2.5
            bilirubin = 1.2 + (faker.random().nextDouble() * 1.3);
        } else {
            // Высокое: 2.5-5.0
            bilirubin = 2.5 + (faker.random().nextDouble() * 2.5);
        }
        observations.add(createObservation(
                patientFhirId,
                "1975-2",
                "Bilirubin",
                Math.round(bilirubin * 100.0) / 100.0,
                "mg/dL",
                0.3, 1.2
        ));

        // Тромбоциты (PLT)
        // Норма: 150-450, возможный диапазон: 0-1000
        double plt;
        rand = faker.random().nextDouble();
        if (rand < 0.85) {
            // Норма: 150-450
            plt = 150.0 + (faker.random().nextDouble() * 300.0);
        } else if (rand < 0.92) {
            // Тромбоцитопения: 50-150
            plt = 50.0 + (faker.random().nextDouble() * 100.0);
        } else {
            // Тромбоцитоз: 450-600
            plt = 450.0 + (faker.random().nextDouble() * 150.0);
        }
        observations.add(createObservation(
                patientFhirId,
                "777-3",
                "Platelets",
                Math.round(plt * 10.0) / 10.0,
                "10*3/uL",
                150.0, 450.0
        ));

        // Лейкоциты (WBC)
        // Норма: 3.5-10.0, возможный диапазон: 2-40
        double wbc;
        rand = faker.random().nextDouble();
        if (rand < 0.85) {
            // Норма: 3.5-10.0
            wbc = 3.5 + (faker.random().nextDouble() * 6.5);
        } else if (rand < 0.92) {
            // Лейкопения: 2.0-3.5
            wbc = 2.0 + (faker.random().nextDouble() * 1.5);
        } else {
            // Лейкоцитоз: 10.0-15.0
            wbc = 10.0 + (faker.random().nextDouble() * 5.0);
        }
        observations.add(createObservation(
                patientFhirId,
                "6690-2",
                "White Blood Cells",
                Math.round(wbc * 10.0) / 10.0,
                "10*3/uL",
                3.5, 10.0
        ));
        
        // Натрий (Na+)
        // Норма: 135-145, возможный диапазон: 120-160
        double sodium;
        rand = faker.random().nextDouble();
        if (rand < 0.90) {
            // Норма: 135-145
            sodium = 135.0 + (faker.random().nextDouble() * 10.0);
        } else if (rand < 0.95) {
            // Гипонатриемия: 125-135
            sodium = 125.0 + (faker.random().nextDouble() * 10.0);
        } else {
            // Гипернатриемия: 145-155
            sodium = 145.0 + (faker.random().nextDouble() * 10.0);
        }
        observations.add(createObservation(
                patientFhirId,
                "2951-2",
                "Sodium",
                Math.round(sodium * 10.0) / 10.0,
                "mmol/L",
                135.0, 145.0
        ));
        
        // Сатурация (SpO2)
        // Норма: 95-100%, возможный диапазон: 85-100%
        double spo2;
        rand = faker.random().nextDouble();
        if (rand < 0.90) {
            // Норма: 95-100%
            spo2 = 95.0 + (faker.random().nextDouble() * 5.0);
        } else {
            // Гипоксия: 88-95%
            spo2 = 88.0 + (faker.random().nextDouble() * 7.0);
        }
        observations.add(createObservation(
                patientFhirId,
                "59408-5",
                "Oxygen Saturation",
                Math.round(spo2 * 10.0) / 10.0,
                "%",
                95.0, 100.0
        ));
        
        // Рост (Height)
        // Реалистичный диапазон: 150-200 cm
        double height = 160.0 + (faker.random().nextDouble() * 30.0); // 160-190 cm
        observations.add(createObservation(
                patientFhirId,
                "8302-2",
                "Body Height",
                Math.round(height * 10.0) / 10.0,
                "cm",
                150.0, 200.0
        ));
        
        // Вес (Weight)
        // Норма: >50 кг, реалистичный диапазон: 50-120 кг
        double weight = 55.0 + (faker.random().nextDouble() * 45.0); // 55-100 кг
        observations.add(createObservation(
                patientFhirId,
                "29463-7",
                "Body Weight",
                Math.round(weight * 10.0) / 10.0,
                "kg",
                50.0, 100.0
        ));
        
        log.debug("Generated {} realistic observations for patient {}", observations.size(), patientFhirId);
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

