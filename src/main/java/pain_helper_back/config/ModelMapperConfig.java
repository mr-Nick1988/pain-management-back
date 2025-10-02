package pain_helper_back.config;

import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pain_helper_back.common.patients.dto.PatientDTO;
import pain_helper_back.common.patients.entity.Patient;

@Configuration
public class ModelMapperConfig {
    @Bean
    ModelMapper getModelMapper() {
        ModelMapper mapper = new ModelMapper();
        mapper.getConfiguration()
                .setFieldMatchingEnabled(true)
                .setFieldAccessLevel(org.modelmapper.config.Configuration.AccessLevel.PRIVATE)
                .setMatchingStrategy(MatchingStrategies.STRICT);

        // Конфигурация для маппинга Patient -> PatientResponseDTO
        mapper.createTypeMap(Patient.class, PatientDTO.class)
                .addMappings(m -> m.map(Patient::getCreatedBy, PatientDTO::setCreatedBy));
// TODO: после внедрения Spring Security и AuditTrail
//       добавить кастомный маппинг: из Person.getLogin() или Person.getId()
//       в PatientDTO.setCreatedBy()
        return mapper;

    }
}
