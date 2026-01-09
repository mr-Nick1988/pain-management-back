package pain_helper_back.doctor.service;


import pain_helper_back.common.patients.dto.*;
import pain_helper_back.common.patients.dto.RecommendationApprovalRejectionDTO;
import pain_helper_back.common.patients.dto.RecommendationWithVasDTO;

import java.time.LocalDate;
import java.util.List;



    public interface DoctorService {

        // ================= PATIENTS ================= //

        /*
         * Созyesние нового patient
         * @param patientDto DTO с yesнными patient
         * @return созyesнный patient
         * @throws EntityExistsException if email or телефон already существуют
         */
        PatientDTO createPatient(PatientDTO patientDto);

        /*
         * retrieval patient по MRN (Medical Record Number)
         * @param mrn уникальный number медицинской карты
         * @return data patient
         * @throws NotFoundException if patient not found
         */
        PatientDTO getPatientByMrn(String mrn);

        /*
         * retrieval patient по email
         * @param email email patient
         * @return data patient
         * @throws NotFoundException if patient not found
         */
        PatientDTO getPatientByEmail(String email);

        /*
         * retrieval patient по numberу телефона
         * @param phoneNumber number телефона patient
         * @return data patient
         * @throws NotFoundException if patient not found
         */
        PatientDTO getPatientByPhoneNumber(String phoneNumber);

        /*
         * search patientов по различным критериям
         * all Parameters опциональны, can комбинировать
         * @param firstName name (частичное совпадение, without учета регистра)
         * @param lastName фамorя (частичное совпадение, without учета регистра)
         * @param isActive status активности (true/false)
         * @param birthDate yesта рождения (точное совпадение)
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
         * Уyesление patient по MRN
         * @param mrn уникальный number медицинской карты
         */
        void deletePatient(String mrn);

        /*
         * update yesнных patient
         * Обновляются only переdata (не null) поля
         * @param mrn уникальный number медицинской карты
         * @param patientUpdateDto DTO с Updateыми полями
         * @return обновленные data patient
         * @throws NotFoundException if patient not found
         */
        PatientDTO updatePatient(String mrn, PatientUpdateDTO patientUpdateDto);

        // ================= EMR (Electronic Medical Records) ================= //

        /*
         * Созyesние новой медицинской карты for patient
         * @param mrn уникальный number медицинской карты patient
         * @param emrDto DTO с медицинскими yesнными
         * @return созyesнная медицинская map
         * @throws NotFoundException if patient not found
         */
        EmrDTO createEmr(String mrn, EmrDTO emrDto);

        /*
         * retrieval afterдней медицинской карты patient
         * @param mrn уникальный number медицинской карты patient
         * @return afterдняя медицинская map
         * @throws NotFoundException if patient not found
         */
        EmrDTO getLastEmrByPatientMrn(String mrn);

        /*
         * update afterдней медицинской карты patient
         * Обновляются only переdata (не null) поля
         * @param mrn уникальный number медицинской карты patient
         * @param emrUpdateDto DTO с Updateыми полями
         * @return обновленная медицинская map
         * @throws NotFoundException if patient not found
         */
        EmrDTO updateEmr(String mrn, EmrUpdateDTO emrUpdateDto);

        /*
         * retrieval allх медицинских карт patient (история)
         * @param mrn уникальный number медицинской карты patient
         * @return list allх медицинских карт patient
         * @throws NotFoundException if patient not found
         */
        List<EmrDTO> getAllEmrByPatientMrn(String mrn);

        // ================= RECOMMENDATIONS (recommendation) ================= //

        /*
         * retrieval allх рекоменyesций со statusом PENDING
         * Returns recommendation вместе с VAS (level pain)
         * @return list рекоменyesций, ожиyesющих одобрения doctorа
         */
        List<RecommendationWithVasDTO> getAllPendingRecommendations();

        /*
         * retrieval afterдней recommendation for patient
         * Returns рекоменyesцию вместе с VAS (level pain)
         * @param mrn уникальный number медицинской карты patient
         * @return afterдняя recommendation с VAS
         * @throws NotFoundException if patient not found
         */
        RecommendationWithVasDTO getLastRecommendationByMrn(String mrn);



        RecommendationDTO approveRecommendation(Long recommendationId, RecommendationApprovalRejectionDTO dto);


        RecommendationDTO rejectRecommendation(Long recommendationId, RecommendationApprovalRejectionDTO dto);


        List<RecommendationWithVasDTO> getRecommendationsWithVasByPatientMrn(String mrn);
    }
