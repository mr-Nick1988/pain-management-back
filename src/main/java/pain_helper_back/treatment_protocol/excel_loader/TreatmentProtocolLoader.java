package pain_helper_back.treatment_protocol.excel_loader;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import pain_helper_back.treatment_protocol.entity.TreatmentProtocol;
import pain_helper_back.treatment_protocol.repository.TreatmentProtocolRepository;
import pain_helper_back.treatment_protocol.utils.SanitizeUtils;

import java.io.InputStream;

@Component
@RequiredArgsConstructor
@Slf4j
public class TreatmentProtocolLoader implements CommandLineRunner {
    private final TreatmentProtocolRepository treatmentProtocolRepository;

    //Apache POI строго типизирован, и если ячейка числовая, он не даст getStringCellValue() и наоборот
    //DataFormatter — это встроенный класс Apache POI, который превращает любую ячейку в строку точно так, как она отображается в Excel

    @Override
    public void run(String... args) throws Exception {
        if (treatmentProtocolRepository.count() > 0) {
            log.info(" Treatment Protocol already loaded");
            return;
        }
        try (InputStream is = new ClassPathResource("treatment_protocol.xlsx").getInputStream(); Workbook workbook = new XSSFWorkbook(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter dataFormatter = new DataFormatter();

            for (Row row : sheet) {
                if (row.getRowNum() == 0) continue;
                if (row.getRowNum() > 22) break; // ограничиваем 22 строки
                TreatmentProtocol treatmentProtocol = new TreatmentProtocol();

                treatmentProtocol.setPainLevel(SanitizeUtils.clean(dataFormatter.formatCellValue(row.getCell(0))));
                treatmentProtocol.setRegimenHierarchy(SanitizeUtils.clean(dataFormatter.formatCellValue(row.getCell(1))));
                treatmentProtocol.setRoute(SanitizeUtils.clean(dataFormatter.formatCellValue(row.getCell(2))));
                treatmentProtocol.setFirstDrug(SanitizeUtils.clean(dataFormatter.formatCellValue(row.getCell(3))));
                treatmentProtocol.setFirstDrugActiveMoiety(SanitizeUtils.clean(dataFormatter.formatCellValue(row.getCell(4))));
                treatmentProtocol.setFirstDosingMg(SanitizeUtils.clean(dataFormatter.formatCellValue(row.getCell(5))));
                treatmentProtocol.setFirstAgeAdjustments(SanitizeUtils.clean(dataFormatter.formatCellValue(row.getCell(6))));
                treatmentProtocol.setFirstIntervalHrs(SanitizeUtils.clean(dataFormatter.formatCellValue(row.getCell(7))));
                treatmentProtocol.setWeightKg(SanitizeUtils.clean(dataFormatter.formatCellValue(row.getCell(8))));
                treatmentProtocol.setFirstChildPugh(SanitizeUtils.clean(dataFormatter.formatCellValue(row.getCell(9))));
                treatmentProtocol.setSecondDrugActiveMoiety(SanitizeUtils.clean(dataFormatter.formatCellValue(row.getCell(10))));
                treatmentProtocol.setSecondDosingMg(SanitizeUtils.clean(dataFormatter.formatCellValue(row.getCell(11))));
                treatmentProtocol.setSecondAgeAdjustments(SanitizeUtils.clean(dataFormatter.formatCellValue(row.getCell(12))));
                treatmentProtocol.setSecondIntervalHrs(SanitizeUtils.clean(dataFormatter.formatCellValue(row.getCell(13))));
                treatmentProtocol.setSecondWeightKg(SanitizeUtils.clean(dataFormatter.formatCellValue(row.getCell(14))));
                treatmentProtocol.setSecondChildPugh(SanitizeUtils.clean(dataFormatter.formatCellValue(row.getCell(15))));
                treatmentProtocol.setGfr(SanitizeUtils.clean(dataFormatter.formatCellValue(row.getCell(16))));
                treatmentProtocol.setPlt(SanitizeUtils.clean(dataFormatter.formatCellValue(row.getCell(17))));
                treatmentProtocol.setWbc(SanitizeUtils.clean(dataFormatter.formatCellValue(row.getCell(18))));
                treatmentProtocol.setSat(SanitizeUtils.clean(dataFormatter.formatCellValue(row.getCell(19))));
                treatmentProtocol.setSodium(SanitizeUtils.clean(dataFormatter.formatCellValue(row.getCell(20))));
                treatmentProtocol.setAvoidIfSensitivity(SanitizeUtils.clean(dataFormatter.formatCellValue(row.getCell(21))));
                treatmentProtocol.setContraindications(SanitizeUtils.clean(dataFormatter.formatCellValue(row.getCell(22))));
                treatmentProtocolRepository.save(treatmentProtocol);
            }
            log.info("Treatment protocol table successfully loaded and sanitized.");
        }
    }






}

//Workbook – интерфейс, представляющий всю книгу Excel.
//XSSFWorkbook – реализация для формата XLSX.
//Что происходит внутри: POI разбирает ZIP/XML структуру XLSX и строит объекты Sheet, Row, Cell в памяти.
//Т.е. сразу в памяти создаётся объектная модель всей книги, но не массив байтов, а именно объектная структура.
// Sheet сам по себе не массив, а объект, который умеет возвращать строки (Row) и ячейки (Cell).

