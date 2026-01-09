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
         * @throws EntityExistsException if email or телефон уже существуют
         */
        PatientDTO createPatient(PatientDTO patientDto);

        /*
         * Получение patient по MRN (Medical Record Number)
         * @param mrn уникальный number медицинской карты
         * @return data patient
         * @throws NotFoundException if patient not found
         */
        PatientDTO getPatientByMrn(String mrn);

        /*
         * Получение patient по email
         * @param email email patient
         * @return data patient
         * @throws NotFoundException if patient not found
         */
        PatientDTO getPatientByEmail(String email);

        /*
         * Получение patient по numberу телефона
         * @param phoneNumber number телефона patient
         * @return data patient
         * @throws NotFoundException if patient not found
         */
        PatientDTO getPatientByPhoneNumber(String phoneNumber);

        /*
         * Поиск patientов по различным критериям
         * all Parameters опциональны, можно комбинировать
         * @param firstName name (частичное совпадение, without учета регистра)
         * @param lastName фамorя (частичное совпадение, without учета регистра)
         * @param isActive status активности (true/false)
         * @param birthDate дата рождения (точное совпадение)
         * @param gender пол (MALE/FEMALE/OTHER)
         * @param insurancePolicyNumber number страховки (частичное совпадение)
         * @param address адрес (частичное совпадение, without учета регистра)
         * @param phoneNumber телефон (частичное совпадение)
         * @param email email (частичное совпадение, without учета регистра)
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
         * @param mrn уникальный number медицинской карты
         */
        void deletePatient(String mrn);

        /*
         * Обновление данных patient
         * Обновляются только переdata (не null) поля
         * @param mrn уникальный number медицинской карты
         * @param patientUpdateDto DTO с Updateыми полями
         * @return обновленные data patient
         * @throws NotFoundException if patient not found
         */
        PatientDTO updatePatient(String mrn, PatientUpdateDTO patientUpdateDto);

        // ================= EMR (Electronic Medical Records) ================= //

        /*
         * Создание новой медицинской карты for patient
         * @param mrn уникальный number медицинской карты patient
         * @param emrDto DTO с медицинскими данными
         * @return созданная медицинская карта
         * @throws NotFoundException if patient not found
         */
        EmrDTO createEmr(String mrn, EmrDTO emrDto);

        /*
         * Получение afterдней медицинской карты patient
         * @param mrn уникальный number медицинской карты patient
         * @return afterдняя медицинская карта
         * @throws NotFoundException if patient not found
         */
        EmrDTO getLastEmrByPatientMrn(String mrn);

        /*
         * Обновление afterдней медицинской карты patient
         * Обновляются только переdata (не null) поля
         * @param mrn уникальный number медицинской карты patient
         * @param emrUpdateDto DTO с Updateыми полями
         * @return обновленная медицинская карта
         * @throws NotFoundException if patient not found
         */
        EmrDTO updateEmr(String mrn, EmrUpdateDTO emrUpdateDto);

        /*
         * Получение allх медицинских карт patient (история)
         * @param mrn уникальный number медицинской карты patient
         * @return list allх медицинских карт patient
         * @throws NotFoundException if patient not found
         */
        List<EmrDTO> getAllEmrByPatientMrn(String mrn);

        // ================= RECOMMENDATIONS (recommendation) ================= //

        /*
         * Получение allх рекомендаций со statusом PENDING
         * Returns recommendation вместе с VAS (уровень боли)
         * @return list рекомендаций, ожидающих одобрения врача
         */
        List<RecommendationWithVasDTO> getAllPendingRecommendations();

        /*
         * Получение afterдней recommendation for patient
         * Returns рекомендацию вместе с VAS (уровень боли)
         * @param mrn уникальный number медицинской карты patient
         * @return afterдняя recommendation с VAS
         * @throws NotFoundException if patient not found
         */
        RecommendationWithVasDTO getLastRecommendationByMrn(String mrn);



        RecommendationDTO approveRecommendation(Long recommendationId, RecommendationApprovalRejectionDTO dto);


        RecommendationDTO rejectRecommendation(Long recommendationId, RecommendationApprovalRejectionDTO dto);


        List<RecommendationWithVasDTO> getRecommendationsWithVasByPatientMrn(String mrn);
    }
