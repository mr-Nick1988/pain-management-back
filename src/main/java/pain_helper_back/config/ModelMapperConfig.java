package pain_helper_back.config;

import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pain_helper_back.common.patients.dto.DiagnosisDTO;
import pain_helper_back.common.patients.dto.PatientDTO;
import pain_helper_back.common.patients.entity.Diagnosis;
import pain_helper_back.common.patients.entity.Patient;

@Configuration
public class ModelMapperConfig {

    @Bean
    ModelMapper getModelMapper() {
        // 🔧 Создаём новый экземпляр ModelMapper
        ModelMapper mapper = new ModelMapper();

        // ⚙️ Базовые настройки маппера
        mapper.getConfiguration()
                // Позволяет ModelMapper работать напрямую с полями класса (а не только с геттерами/сеттерами)
                .setFieldMatchingEnabled(true)
                // Разрешает доступ к приватным полям через reflection
                .setFieldAccessLevel(org.modelmapper.config.Configuration.AccessLevel.PRIVATE)
                // Устанавливает максимально строгую стратегию сопоставления:
                // поля должны полностью совпадать по имени и типу, иначе будут проигнорированы
                .setMatchingStrategy(MatchingStrategies.STRICT);

        // 🧩 Кастомная карта для Patient → PatientDTO
        // Используется при возврате DTO наружу, чтобы скопировать нужные поля вручную
        mapper.createTypeMap(Patient.class, PatientDTO.class)
                // Пример явного маппинга (если имена не совпадают, ModelMapper без этого их бы не увидел)
                .addMappings(m -> m.map(Patient::getCreatedBy, PatientDTO::setCreatedBy));

        // 🧠 Кастомная карта для DiagnosisDTO → Diagnosis
        // Это ключевой маппинг, без него ModelMapper не мапил бы коллекцию диагнозов внутри EMR
        mapper.createTypeMap(DiagnosisDTO.class, Diagnosis.class)
                .addMappings(m -> {
                    // Маппинг кода болезни (ICD)
                    m.map(DiagnosisDTO::getIcdCode, Diagnosis::setIcdCode);
                    // Маппинг описания болезни
                    m.map(DiagnosisDTO::getDescription, Diagnosis::setDescription);
                });

        // ⚠️ TODO (будущее улучшение):
        // После подключения Spring Security можно добавить маппинг для аудита:
        // например, брать логин текущего пользователя и писать его в createdBy.

        // ✅ Возвращаем готовый, полностью настроенный экземпляр
        return mapper;
    }
}