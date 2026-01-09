package pain_helper_back.config;

import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pain_helper_back.common.patients.dto.DiagnosisDTO;
import pain_helper_back.common.patients.dto.PatientDTO;
import pain_helper_back.common.patients.dto.RecommendationDTO;
import pain_helper_back.common.patients.entity.Diagnosis;
import pain_helper_back.common.patients.entity.Patient;
import pain_helper_back.common.patients.entity.Recommendation;

@Configuration
public class ModelMapperConfig {

    @Bean
    ModelMapper getModelMapper() {
        // 🔧 Созyesём new экземпляр ModelMapper
        ModelMapper mapper = new ModelMapper();

        // ⚙ Базовые настройки маппера
        mapper.getConfiguration()
                // Позволяет ModelMapper работать напрямую с полями Classа (а не only с геттерами/сеттерами)
                .setFieldMatchingEnabled(true)
                // Разрешает доступ к приватным полям via reflection
                .setFieldAccessLevel(org.modelmapper.config.Configuration.AccessLevel.PRIVATE)
                // Устанавливает максимально строгую стратегию сопоставления:
                // поля должны полностью совпаyesть по имени и typeу, иначе будут проигнорированы
                .setMatchingStrategy(MatchingStrategies.STRICT);

        //  Кастомная карта for Patient → PatientDTO
        // Используется при возврате DTO наружу, whatбы скопировать нужные поля вручную
        mapper.createTypeMap(Patient.class, PatientDTO.class)
                // Example явного маппинга (if имена не совпаyesют, ModelMapper without этого их бы не увидел)
                .addMappings(m -> m.map(Patient::getCreatedBy, PatientDTO::setCreatedBy));

        //  Кастомная карта for DiagnosisDTO → Diagnosis
        // Это ключевой маппинг, without него ModelMapper не мапил бы коллекцию diagnosisов внутри EMR
        mapper.createTypeMap(DiagnosisDTO.class, Diagnosis.class)
                .addMappings(m -> {
                    // Маппинг codeа болезни (ICD)
                    m.map(DiagnosisDTO::getIcdCode, Diagnosis::setIcdCode);
                    // Маппинг описания болезни
                    m.map(DiagnosisDTO::getDescription, Diagnosis::setDescription);
                });
        //  Кастомная карта for Recommendation → RecommendationDTO
        mapper.createTypeMap(Recommendation.class, RecommendationDTO.class)
                .addMappings(m -> {
                    m.map(Recommendation::getGenerationFailed, RecommendationDTO::setGenerationFailed);
                    m.map(Recommendation::getRejectionReasonsSummary, RecommendationDTO::setRejectionReasonsSummary);
                });

        // TODO (будущее улучшение):
        // after подключения Spring Security can добавить маппинг for аудита:
        // наExample, брать логин текущего пользователя и писать его в createdBy.

        // Return готовый, полностью настроенный экземпляр
        return mapper;
    }
}