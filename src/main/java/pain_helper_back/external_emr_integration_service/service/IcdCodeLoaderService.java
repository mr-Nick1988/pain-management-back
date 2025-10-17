package pain_helper_back.external_emr_integration_service.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Сервис для загрузки ICD кодов диагнозов из CSV файла.
 * 
 * НАЗНАЧЕНИЕ:
 * - Загружает коды болезней из icd9cm_2015_converted.csv при старте приложения
 * - Предоставляет случайные диагнозы для моковых пациентов
 * - Кэширует данные в памяти для быстрого доступа
 * 
 * ФОРМАТ CSV:
 * code,name
 * 001.0,Cholera due to vibrio cholerae
 * 250.00,Diabetes mellitus without mention of complication
 */
@Service
@Slf4j
public class IcdCodeLoaderService {
    
    private List<IcdCode> icdCodes = new ArrayList<>();
    private Random random = new Random();
    
    /**
     * Загружает ICD коды при старте приложения.
     */
    @PostConstruct
    public void loadIcdCodes() {
        log.info("Loading ICD codes from CSV...");
        
        try {
            ClassPathResource resource = new ClassPathResource("icd9cm_2015_converted.csv");
            
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
                
                // Пропускаем заголовок
                String headerLine = reader.readLine();
                
                String line;
                int count = 0;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split(",", 2);
                    if (parts.length == 2) {
                        String code = parts[0].trim();
                        String name = parts[1].trim().replace("\"", "");
                        icdCodes.add(new IcdCode(code, name));
                        count++;
                    }
                }
                
                log.info("Successfully loaded {} ICD codes", count);
            }
            
        } catch (Exception e) {
            log.error("Failed to load ICD codes: {}", e.getMessage(), e);
            // Добавляем несколько дефолтных кодов для работоспособности
            addDefaultCodes();
        }
    }
    
    /**
     * Возвращает случайный диагноз.
     */
    public IcdCode getRandomDiagnosis() {
        if (icdCodes.isEmpty()) {
            return new IcdCode("999.9", "Unknown diagnosis");
        }
        return icdCodes.get(random.nextInt(icdCodes.size()));
    }
    
    /**
     * Возвращает список случайных диагнозов.
     * 
     * @param count количество диагнозов (1-5)
     */
    public List<IcdCode> getRandomDiagnoses(int count) {
        if (icdCodes.isEmpty()) {
            return List.of(new IcdCode("999.9", "Unknown diagnosis"));
        }
        
        // Ограничиваем количество диагнозов
        int actualCount = Math.min(count, 5);
        actualCount = Math.max(actualCount, 1);
        
        List<IcdCode> selected = new ArrayList<>();
        Set<Integer> usedIndices = new HashSet<>();
        
        while (selected.size() < actualCount && usedIndices.size() < icdCodes.size()) {
            int index = random.nextInt(icdCodes.size());
            if (!usedIndices.contains(index)) {
                selected.add(icdCodes.get(index));
                usedIndices.add(index);
            }
        }
        
        return selected;
    }
    
    /**
     * Добавляет дефолтные коды на случай ошибки загрузки.
     */
    private void addDefaultCodes() {
        icdCodes.add(new IcdCode("250.00", "Diabetes mellitus without mention of complication"));
        icdCodes.add(new IcdCode("401.9", "Essential hypertension, unspecified"));
        icdCodes.add(new IcdCode("272.4", "Hyperlipidemia"));
        icdCodes.add(new IcdCode("530.81", "Gastroesophageal reflux disease"));
        icdCodes.add(new IcdCode("715.90", "Osteoarthrosis, unspecified"));
        log.warn("Using default ICD codes due to loading error");
    }
    
    /**
     * DTO для ICD кода.
     */
    public static class IcdCode {
        private final String code;
        private final String description;
        
        public IcdCode(String code, String description) {
            this.code = code;
            this.description = description;
        }
        
        public String getCode() {
            return code;
        }
        
        public String getDescription() {
            return description;
        }
    }
}
