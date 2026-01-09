package pain_helper_back.doctor.service;


import pain_helper_back.common.patients.dto.*;
import pain_helper_back.common.patients.dto.RecommendationApprovalRejectionDTO;
import pain_helper_back.common.patients.dto.RecommendationWithVasDTO;

import java.time.LocalDate;
import java.util.List;



    public interface DoctorService {

        // ================= PATIENTS ================= //

        /*
         * Создание нового patient
         * @param patientDto DTO с данными patient
         * @return созданный patient
         * @throws EntityExistsException если email или телефон уже существуют
         */
        PatientDTO createPatient(PatientDTO patientDto);

        /*
         * Получение patient по MRN (Medical Record Number)
         * @param mrn уникальный номер медицинской карты
         * @return data patient
         * @throws NotFoundException если patient not found
         */
        PatientDTO getPatientByMrn(String mrn);

        /*
         * Получение patient по email
         * @param email email patient
         * @return data patient
         * @throws NotFoundException если patient not found
         */
        PatientDTO getPatientByEmail(String email);

        /*
         * Получение patient по номеру телефона
         * @param phoneNumber номер телефона patient
         * @return data patient
         * @throws NotFoundException если patient not found
         */
        PatientDTO getPatientByPhoneNumber(String phoneNumber);

        /*
         * Поиск patientов по различным критериям
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
         * @return list найденных patientов
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
         * Удаление patient по MRN
         * @param mrn уникальный номер медицинской карты
         */
        void deletePatient(String mrn);

        /*
         * Обновление данных patient
         * Обновляются только переdata (не null) поля
         * @param mrn уникальный номер медицинской карты
         * @param patientUpdateDto DTO с Updateыми полями
         * @return обновленные data patient
         * @throws NotFoundException если patient not found
         */
        PatientDTO updatePatient(String mrn, PatientUpdateDTO patientUpdateDto);

        // ================= EMR (Electronic Medical Records) ================= //

        /*
         * Создание новой медицинской карты для patient
         * @param mrn уникальный номер медицинской карты patient
         * @param emrDto DTO с медицинскими данными
         * @return созданная медицинская карта
         * @throws NotFoundException если patient not found
         */
        EmrDTO createEmr(String mrn, EmrDTO emrDto);

        /*
         * Получение последней медицинской карты patient
         * @param mrn уникальный номер медицинской карты patient
         * @return последняя медицинская карта
         * @throws NotFoundException если patient not found
         */
        EmrDTO getLastEmrByPatientMrn(String mrn);

        /*
         * Обновление последней медицинской карты patient
         * Обновляются только переdata (не null) поля
         * @param mrn уникальный номер медицинской карты patient
         * @param emrUpdateDto DTO с Updateыми полями
         * @return обновленная медицинская карта
         * @throws NotFoundException если patient not found
         */
        EmrDTO updateEmr(String mrn, EmrUpdateDTO emrUpdateDto);

        /*
         * Получение всех медицинских карт patient (история)
         * @param mrn уникальный номер медицинской карты patient
         * @return list всех медицинских карт patient
         * @throws NotFoundException если patient not found
         */
        List<EmrDTO> getAllEmrByPatientMrn(String mrn);

        // ================= RECOMMENDATIONS (recommendation) ================= //

        /*
         * Получение всех рекомендаций со статусом PENDING
         * Возвращает recommendation вместе с VAS (уровень боли)
         * @return list рекомендаций, ожидающих одобрения врача
         */
        List<RecommendationWithVasDTO> getAllPendingRecommendations();

        /*
         * Получение последней recommendation для patient
         * Возвращает рекомендацию вместе с VAS (уровень боли)
         * @param mrn уникальный номер медицинской карты patient
         * @return последняя recommendation с VAS
         * @throws NotFoundException если patient not found
         */
        RecommendationWithVasDTO getLastRecommendationByMrn(String mrn);



        RecommendationDTO approveRecommendation(Long recommendationId, RecommendationApprovalRejectionDTO dto);


        RecommendationDTO rejectRecommendation(Long recommendationId, RecommendationApprovalRejectionDTO dto);


        List<RecommendationWithVasDTO> getRecommendationsWithVasByPatientMrn(String mrn);
    }
