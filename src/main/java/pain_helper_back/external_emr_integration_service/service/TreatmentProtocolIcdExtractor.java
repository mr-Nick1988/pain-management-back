package pain_helper_back.external_emr_integration_service.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pain_helper_back.treatment_protocol.entity.TreatmentProtocol;
import pain_helper_back.treatment_protocol.repository.TreatmentProtocolRepository;
import pain_helper_back.treatment_protocol.utils.SanitizeUtils;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Map;

/**
 * Сервис для извлечения ICD кодов из Treatment Protocol.
 * 
 * НАЗНАЧЕНИЕ:
 * - Извлекает все ICD коды из колонки contraindications в treatment_protocol
 * - Предоставляет только те коды, которые реально используются в протоколах лечения
 * - Моковые пациенты создаются ТОЛЬКО с этими кодами
 * 
 * ЗАЧЕМ:
 * - Раньше моковые пациенты создавались с любыми случайными диагнозами из 14,000+ кодов
 * - Теперь они создаются только с теми диагнозами, которые влияют на выбор лечения
 * - Это делает тестирование более реалистичным и предсказуемым
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TreatmentProtocolIcdExtractor {
    
    private final TreatmentProtocolRepository treatmentProtocolRepository;
    private final IcdCodeLoaderService icdCodeLoaderService;
    
    private List<IcdCodeLoaderService.IcdCode> protocolIcdCodes = new ArrayList<>();
    private Random random = new Random();
    
    /**
     * Регулярное выражение для извлечения ICD-кодов (например: 571.201, V45.1103, E11.9 и т.п.)
     */
    private static final Pattern ICD_PATTERN = Pattern.compile("[A-Z]?[0-9]{3}(?:\\.[0-9A-Z]+)?");
    
    /**
     * Извлекает ICD коды из Treatment Protocol после загрузки базы данных.
     */
    @PostConstruct
    public void extractIcdCodesFromProtocol() {
        log.info("Extracting ICD codes from Treatment Protocol contraindications...");
        
        try {
            // Получаем все протоколы лечения
            List<TreatmentProtocol> protocols = treatmentProtocolRepository.findAll();
            
            if (protocols.isEmpty()) {
                log.warn("No treatment protocols found in database. Using default ICD codes.");
                return;
            }
            
            Set<String> uniqueIcdCodes = new HashSet<>();
            
            // Извлекаем ICD коды из каждого протокола
            for (TreatmentProtocol protocol : protocols) {
                String contraindications = protocol.getContraindications();
                
                if (contraindications == null || contraindications.trim().isEmpty() 
                        || contraindications.equalsIgnoreCase("NA")) {
                    continue;
                }
                
                // Санитизируем строку
                String cleaned = SanitizeUtils.clean(contraindications);
                
                // Извлекаем ICD коды
                Matcher matcher = ICD_PATTERN.matcher(cleaned);
                while (matcher.find()) {
                    String code = matcher.group().trim().toUpperCase();
                    uniqueIcdCodes.add(code);
                }
            }
            
            log.info("Found {} unique ICD codes in Treatment Protocol", uniqueIcdCodes.size());
            
            // Логируем все найденные contraindications для отладки
            log.info("=== ALL CONTRAINDICATIONS FROM TREATMENT PROTOCOL ===");
            for (TreatmentProtocol protocol : protocols) {
                String contra = protocol.getContraindications();
                if (contra != null && !contra.trim().isEmpty() && !contra.equalsIgnoreCase("NA")) {
                    log.info("Protocol row: {}", contra);
                }
            }
            log.info("=== END OF CONTRAINDICATIONS ===");
            
            // Создаем список IcdCode объектов с описаниями
            for (String code : uniqueIcdCodes) {
                String description = findDescriptionForCode(code);
                protocolIcdCodes.add(new IcdCodeLoaderService.IcdCode(code, description));
            }
            
            log.info("Successfully extracted {} ICD codes from Treatment Protocol:", protocolIcdCodes.size());
            log.info("=== UNIQUE ICD CODES ===");
            protocolIcdCodes.forEach(icd -> log.info("  - {} : {}", icd.getCode(), icd.getDescription()));
            log.info("=== END OF UNIQUE ICD CODES ===");
            
        } catch (Exception e) {
            log.error("Failed to extract ICD codes from Treatment Protocol: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Возвращает случайный диагноз из протокола лечения.
     * Если протокол пуст, использует полный список ICD кодов.
     */
    public IcdCodeLoaderService.IcdCode getRandomProtocolDiagnosis() {
        if (protocolIcdCodes.isEmpty()) {
            log.warn("No protocol ICD codes available, falling back to full ICD code list");
            return icdCodeLoaderService.getRandomDiagnosis();
        }
        return protocolIcdCodes.get(random.nextInt(protocolIcdCodes.size()));
    }
    
    /**
     * Возвращает список случайных диагнозов из протокола лечения.
     * Если протокол пуст, использует полный список ICD кодов.
     * 
     * @param count количество диагнозов (1-5)
     */
    public List<IcdCodeLoaderService.IcdCode> getRandomProtocolDiagnoses(int count) {
        if (protocolIcdCodes.isEmpty()) {
            log.warn("No protocol ICD codes available, falling back to full ICD code list");
            return icdCodeLoaderService.getRandomDiagnoses(count);
        }
        
        // Ограничиваем количество диагнозов
        int actualCount = Math.min(count, 5);
        actualCount = Math.max(actualCount, 1);
        actualCount = Math.min(actualCount, protocolIcdCodes.size());
        
        List<IcdCodeLoaderService.IcdCode> selected = new ArrayList<>();
        Set<Integer> usedIndices = new HashSet<>();
        
        while (selected.size() < actualCount && usedIndices.size() < protocolIcdCodes.size()) {
            int index = random.nextInt(protocolIcdCodes.size());
            if (!usedIndices.contains(index)) {
                selected.add(protocolIcdCodes.get(index));
                usedIndices.add(index);
            }
        }
        
        return selected;
    }
    
    /**
     * Находит описание для ICD кода.
     * Создает понятное описание на основе кода.
     */
    private String findDescriptionForCode(String code) {
        // Маппинг известных кодов из Treatment Protocol
        Map<String, String> knownDescriptions = Map.ofEntries(
            Map.entry("571.2", "Alcoholic cirrhosis of liver"),
            Map.entry("571.5", "Cirrhosis of liver without mention of alcohol"),
            Map.entry("571.9", "Unspecified chronic liver disease"),
            Map.entry("V45.11", "Renal dialysis status"),
            Map.entry("E11.9", "Type 2 diabetes mellitus without complications"),
            Map.entry("I50.9", "Heart failure, unspecified"),
            Map.entry("K70.3", "Alcoholic cirrhosis of liver"),
            Map.entry("K74.6", "Other and unspecified cirrhosis of liver"),
            Map.entry("N18.6", "End stage renal disease"),
            Map.entry("Z99.2", "Dependence on renal dialysis")
        );
        
        // Пытаемся найти точное совпадение
        String description = knownDescriptions.get(code);
        if (description != null) {
            return description;
        }
        
        // Пытаемся найти по базовому коду (571.201 -> 571.2)
        String baseCode = getBaseCode(code);
        description = knownDescriptions.get(baseCode);
        if (description != null) {
            return description + " (variant: " + code + ")";
        }
        
        // Если не найдено - возвращаем generic описание
        return "Contraindication condition (ICD: " + code + ")";
    }
    
    /**
     * Возвращает "базовую часть" ICD-кода — до точки и одной цифры после.
     */
    private String getBaseCode(String code) {
        if (code == null) return "";
        int dotIndex = code.indexOf('.');
        if (dotIndex != -1 && dotIndex + 2 <= code.length()) {
            return code.substring(0, Math.min(dotIndex + 2, code.length()));
        }
        return code;
    }
    
    /**
     * Возвращает количество доступных ICD кодов из протокола.
     */
    public int getProtocolIcdCodesCount() {
        return protocolIcdCodes.size();
    }
}
