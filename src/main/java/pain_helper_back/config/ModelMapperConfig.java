package pain_helper_back.config;

import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pain_helper_back.doctor.dto.PatientResponseDTO;
import pain_helper_back.doctor.entity.Patient;

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
        mapper.createTypeMap(Patient.class, PatientResponseDTO.class)
                .addMappings(m -> m.map(src -> src.getCreatedBy().getId(), PatientResponseDTO::setCreatedBy));

        return mapper;

    }
}
