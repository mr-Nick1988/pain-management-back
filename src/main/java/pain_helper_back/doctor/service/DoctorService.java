package pain_helper_back.doctor.service;


import pain_helper_back.common.patients.dto.*;
import pain_helper_back.doctor.dto.RecommendationApprovalRejectionDTO;
import pain_helper_back.doctor.dto.RecommendationWithVasDTO;

import java.time.LocalDate;
import java.util.List;



    public interface DoctorService {

        // ================= PATIENTS ================= //

        /*
         * Создание нового пациента
         * @param patientDto DTO с данными пациента
         * @return созданный пациент
         * @throws EntityExistsException если email или телефон уже существуют
         */
        PatientDTO createPatient(PatientDTO patientDto);

        /*
         * Получение пациента по MRN (Medical Record Number)
         * @param mrn уникальный номер медицинской карты
         * @return данные пациента
         * @throws NotFoundException если пациент не найден
         */
        PatientDTO getPatientByMrn(String mrn);

        /*
         * Получение пациента по email
         * @param email email пациента
         * @return данные пациента
         * @throws NotFoundException если пациент не найден
         */
        PatientDTO getPatientByEmail(String email);

        /*
         * Получение пациента по номеру телефона
         * @param phoneNumber номер телефона пациента
         * @return данные пациента
         * @throws NotFoundException если пациент не найден
         */
        PatientDTO getPatientByPhoneNumber(String phoneNumber);

        /*
         * Поиск пациентов по различным критериям
         * Все параметры опциональны, можно комбинировать
         * @param firstName имя (частичное совпадение, без учета регистра)
         * @param lastName фамилия (частичное совпадение, без учета регистра)
         * @param isActive статус активности (true/false)
         * @param birthDate дата рождения (точное совпадение)
         * @param gender пол (MALE/FEMALE/OTHER)
         * @param insurancePolicyNumber номер страховки (частичное совпадение)
         * @param address адрес (частичное совпадение, без учета регистра)
         * @param phoneNumber телефон (частичное совпадение)
         * @param email email (частичное совпадение, без учета регистра)
         * @return список найденных пациентов
         */
        List<PatientDTO> searchPatients(
                String firstName,
                String lastName,
                Boolean isActive,
                LocalDate birthDate,
                String gender,
                String insurancePolicyNumber,
                String address,
                String phoneNumber,
                String email
        );

        /*
         * Удаление пациента по MRN
         * @param mrn уникальный номер медицинской карты
         */
        void deletePatient(String mrn);

        /*
         * Обновление данных пациента
         * Обновляются только переданные (не null) поля
         * @param mrn уникальный номер медицинской карты
         * @param patientUpdateDto DTO с обновляемыми полями
         * @return обновленные данные пациента
         * @throws NotFoundException если пациент не найден
         */
        PatientDTO updatePatient(String mrn, PatientUpdateDTO patientUpdateDto);

        // ================= EMR (Electronic Medical Records) ================= //

        /*
         * Создание новой медицинской карты для пациента
         * @param mrn уникальный номер медицинской карты пациента
         * @param emrDto DTO с медицинскими данными
         * @return созданная медицинская карта
         * @throws NotFoundException если пациент не найден
         */
        EmrDTO createEmr(String mrn, EmrDTO emrDto);

        /*
         * Получение последней медицинской карты пациента
         * @param mrn уникальный номер медицинской карты пациента
         * @return последняя медицинская карта
         * @throws NotFoundException если пациент не найден
         */
        EmrDTO getLastEmrByPatientMrn(String mrn);

        /*
         * Обновление последней медицинской карты пациента
         * Обновляются только переданные (не null) поля
         * @param mrn уникальный номер медицинской карты пациента
         * @param emrUpdateDto DTO с обновляемыми полями
         * @return обновленная медицинская карта
         * @throws NotFoundException если пациент не найден
         */
        EmrDTO updateEmr(String mrn, EmrUpdateDTO emrUpdateDto);

        /*
         * Получение всех медицинских карт пациента (история)
         * @param mrn уникальный номер медицинской карты пациента
         * @return список всех медицинских карт пациента
         * @throws NotFoundException если пациент не найден
         */
        List<EmrDTO> getAllEmrByPatientMrn(String mrn);

        // ================= RECOMMENDATIONS (Рекомендации) ================= //

        /*
         * Получение всех рекомендаций со статусом PENDING
         * Возвращает рекомендации вместе с VAS (уровень боли)
         * @return список рекомендаций, ожидающих одобрения врача
         */
        List<RecommendationWithVasDTO> getAllPendingRecommendations();

        /*
         * Получение последней рекомендации для пациента
         * Возвращает рекомендацию вместе с VAS (уровень боли)
         * @param mrn уникальный номер медицинской карты пациента
         * @return последняя рекомендация с VAS
         * @throws NotFoundException если пациент не найден
         */
        RecommendationWithVasDTO getLastRecommendationByMrn(String mrn);

        // ================= WORKFLOW: APPROVAL/REJECTION ================= //

        /*
         * Одобрение рекомендации врачом
         *
         * WORKFLOW:
         * 1. Проверяет, что рекомендация в статусе PENDING
         * 2. Меняет статус: PENDING → APPROVED_BY_DOCTOR
         * 3. Сохраняет ID врача, время одобрения и комментарий
         * 4. Меняет статус: APPROVED_BY_DOCTOR → FINAL_APPROVED
         * 5. Сохраняет финальное одобрение
         *
         * @param mrn уникальный номер медицинской карты пациента
         * @param dto DTO с комментарием врача (опционально)
         * @return обновленная рекомендация
         * @throws NotFoundException если пациент не найден
         * @throws IllegalStateException если рекомендация не в статусе PENDING
         */
        RecommendationDTO approveRecommendation(Long recommendationId, RecommendationApprovalRejectionDTO dto);

        /*
         * Отклонение рекомендации врачом с автоматической эскалацией
         *
         * WORKFLOW:
         * 1. Проверяет, что рекомендация в статусе PENDING
         * 2. Меняет статус: PENDING → REJECTED_BY_DOCTOR
         * 3. Сохраняет ID врача, время отклонения, комментарий и причину отказа
         * 4. СОЗДАЕТ ЭСКАЛАЦИЮ:
         *    - Связывает с рекомендацией
         *    - Определяет приоритет по уровню боли (VAS):
         *      * VAS >= 8 → HIGH
         *      * VAS >= 5 → MEDIUM
         *      * VAS < 5 → LOW
         *    - Статус эскалации: PENDING
         * 5. Меняет статус рекомендации: REJECTED_BY_DOCTOR → ESCALATED_TO_ANESTHESIOLOGIST
         * 6. Сохраняет рекомендацию (cascade сохранит эскалацию)
         *
         * @param mrn уникальный номер медицинской карты пациента
         * @param dto DTO с причиной отказа (обязательно) и комментарием (опционально)
         * @return обновленная рекомендация с эскалацией
         * @throws NotFoundException если пациент не найден
         * @throws IllegalStateException если рекомендация не в статусе PENDING
         * @throws IllegalArgumentException если не указана причина отказа
         */
        RecommendationDTO rejectRecommendation(Long recommendationId, RecommendationApprovalRejectionDTO dto);


}
