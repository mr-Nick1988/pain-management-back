package pain_helper_back.treatment_protocol.excel_loader;

import lombok.RequiredArgsConstructor;
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

import java.io.InputStream;

@Component
@RequiredArgsConstructor
public class TreatmentProtocolLoader implements CommandLineRunner {
    private final TreatmentProtocolRepository treatmentProtocolRepository;

    //Apache POI строго типизирован, и если ячейка числовая, он не даст getStringCellValue() и наоборот
    //DataFormatter — это встроенный класс Apache POI, который превращает любую ячейку в строку точно так, как она отображается в Excel

    @Override
    public void run(String... args) throws Exception {
        try (InputStream is = new ClassPathResource("treatment_protocol.xlsx").getInputStream(); Workbook workbook = new XSSFWorkbook(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter dataFormatter = new DataFormatter();

            for (Row row : sheet) {
                if (row.getRowNum() == 0) continue;
                TreatmentProtocol treatmentProtocol = new TreatmentProtocol();
                treatmentProtocol.setPainLevel(dataFormatter.formatCellValue(row.getCell(0)));
                treatmentProtocol.setRegimenHierarchy(dataFormatter.formatCellValue(row.getCell(1)));
                treatmentProtocol.setRoute(dataFormatter.formatCellValue(row.getCell(2)));
                treatmentProtocol.setFirstDrug(dataFormatter.formatCellValue(row.getCell(3)));
                treatmentProtocol.setFirstDrugActiveMoiety(dataFormatter.formatCellValue(row.getCell(4)));
                treatmentProtocol.setFirstDosingMg(dataFormatter.formatCellValue(row.getCell(5)));
                treatmentProtocol.setFirstAgeAdjustments(dataFormatter.formatCellValue(row.getCell(6)));
                treatmentProtocol.setFirstIntervalHrs(dataFormatter.formatCellValue(row.getCell(7)));
                treatmentProtocol.setWeightKg(dataFormatter.formatCellValue(row.getCell(8)));
                treatmentProtocol.setFirstChildPugh(dataFormatter.formatCellValue(row.getCell(9)));
                treatmentProtocol.setSecondDrugActiveMoiety(dataFormatter.formatCellValue(row.getCell(10)));
                treatmentProtocol.setSecondDosingMg(dataFormatter.formatCellValue(row.getCell(11)));
                treatmentProtocol.setSecondAgeAdjustments(dataFormatter.formatCellValue(row.getCell(12)));
                treatmentProtocol.setSecondIntervalHrs(dataFormatter.formatCellValue(row.getCell(13)));
                treatmentProtocol.setSecondWeightKg(dataFormatter.formatCellValue(row.getCell(14)));
                treatmentProtocol.setSecondChildPugh(dataFormatter.formatCellValue(row.getCell(15)));
                treatmentProtocol.setGfr(dataFormatter.formatCellValue(row.getCell(16)));
                treatmentProtocol.setPlt(dataFormatter.formatCellValue(row.getCell(17)));
                treatmentProtocol.setWbc(dataFormatter.formatCellValue(row.getCell(18)));
                treatmentProtocol.setSat(dataFormatter.formatCellValue(row.getCell(19)));
                treatmentProtocol.setSodium(dataFormatter.formatCellValue(row.getCell(20)));
                treatmentProtocol.setAvoidIfSensitivity(dataFormatter.formatCellValue(row.getCell(21)));
                treatmentProtocol.setContraindications(dataFormatter.formatCellValue(row.getCell(22)));

                treatmentProtocolRepository.save(treatmentProtocol);
                System.out.println(treatmentProtocol);
            }
        }
    }
}

//Workbook – интерфейс, представляющий всю книгу Excel.
//XSSFWorkbook – реализация для формата XLSX.
//Что происходит внутри: POI разбирает ZIP/XML структуру XLSX и строит объекты Sheet, Row, Cell в памяти.
//Т.е. сразу в памяти создаётся объектная модель всей книги, но не массив байтов, а именно объектная структура.
// Sheet сам по себе не массив, а объект, который умеет возвращать строки (Row) и ячейки (Cell).

