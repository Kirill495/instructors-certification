package org.tourism.instructors.application.reports;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.tourism.instructors.domain.protocol.repository.ProtocolRepository;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class ReportService {

    private static final LocalDate MIN_DATE = LocalDate.of(1900, 1, 1);
    private static final LocalDate MAX_DATE = LocalDate.of(9999, 12, 31);

    private final ProtocolRepository protocolRepository;

    public ReportService (ProtocolRepository protocolRepository) {
        this.protocolRepository = protocolRepository;
    }

    public byte[] exportProtocols(Optional<LocalDate> dateFrom, Optional<LocalDate> dateTill) throws IOException {

        try (Workbook workbook = new XSSFWorkbook();
            ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Протоколы");

            // Header row
            createHeader(sheet);
            CellStyle dateStyle = workbook.createCellStyle();
            dateStyle.setDataFormat(workbook.getCreationHelper().createDataFormat().getFormat("dd.MM.yyyy"));

            List<ProtocolRepository.ReportAssignmentProjection> data = protocolRepository.getAssignmentsForReport(
                    dateFrom.orElse(MIN_DATE),
                    dateTill.orElse(MAX_DATE));
            for (int i = 0; i < data.size(); i++) {
                Row row = sheet.createRow(i + 1);
                ProtocolRepository.ReportAssignmentProjection item = data.get(i);
                row.createCell(0).setCellValue(item.getLastName());
                row.createCell(1).setCellValue(item.getFirstName());
                row.createCell(2).setCellValue(item.getMiddleName());
                row.createCell(3).setCellValue(item.getClub());
                row.createCell(4).setCellValue(item.getGradeTitle());
                row.createCell(5).setCellValue(item.getKindOfTourismTitle());
                Cell dateCell = row.createCell(6);
                dateCell.setCellValue(item.getAssignmentDate());
                dateCell.setCellStyle(dateStyle);
                Cell expiresCell = row.createCell(7);
                expiresCell.setCellValue(item.getAssignmentDate().plusYears(item.getExpiresInYears()));
                expiresCell.setCellStyle(dateStyle);
                row.createCell(8).setCellValue("ФСТ ОТМ");
            }
            for (int i = 0; i < 9; i++) {
                sheet.autoSizeColumn(i);
            }
            sheet.setColumnWidth(3, Math.min(sheet.getColumnWidth(3), 15 * 256));
            workbook.write(out);
            return out.toByteArray();
        }
    }

    private static void createHeader (Sheet sheet) {
        CellStyle headerStyle = createHeaderStyle(sheet.getWorkbook());

        Row header = sheet.createRow(0);
        String[] columns = {"Фамилия", "Имя", "Отчество", "Клуб", "Звание", "Вид туризма", "Дата аттестации", "Срок окончания", "Федерация"};
        for (int i = 0; i < columns.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(columns[i]);
            cell.setCellStyle(headerStyle);
        }
        sheet.createFreezePane(0, 1);
        sheet.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, columns.length - 1));
    }

    private static @NonNull CellStyle createHeaderStyle (Workbook workbook) {
        CellStyle headerStyle = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        headerStyle.setFont(font);
        return headerStyle;
    }
}
