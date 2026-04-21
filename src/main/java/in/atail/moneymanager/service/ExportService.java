package in.atail.moneymanager.service;

import in.atail.moneymanager.entity.ExpenseEntity;
import in.atail.moneymanager.entity.IncomeEntity;
import in.atail.moneymanager.repository.ExpenseRepository;
import in.atail.moneymanager.repository.IncomeRepository;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExportService {

    private final IncomeRepository incomeRepository;
    private final ExpenseRepository expenseRepository;
    private final ProfileService profileService;

    public byte[] exportTransactions() throws IOException {
        Long profileId = profileService.getCurrentProfileId();
        List<ExportRow> rows = new ArrayList<>();

        incomeRepository.findByProfileIdOrderByDateDesc(profileId).forEach(income ->
                rows.add(new ExportRow(
                        income.getName(),
                        income.getAmount(),
                        income.getCategory() != null ? income.getCategory().getName() : "",
                        income.getDate(),
                        "收入"
                ))
        );

        expenseRepository.findByProfileIdOrderByDateDesc(profileId).forEach(expense ->
                rows.add(new ExportRow(
                        expense.getName(),
                        expense.getAmount(),
                        expense.getCategory() != null ? expense.getCategory().getName() : "",
                        expense.getDate(),
                        "支出"
                ))
        );

        rows.sort(Comparator.comparing(ExportRow::getDate).reversed());

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Transactions");
            createHeader(workbook, sheet);
            fillRows(sheet, rows);
            autoSizeColumns(sheet);
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    private void createHeader(Workbook workbook, Sheet sheet) {
        Row headerRow = sheet.createRow(0);
        String[] headers = {"名称", "金额", "分类", "日期", "类型"};

        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);

        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
    }

    private void fillRows(Sheet sheet, List<ExportRow> rows) {
        int rowIndex = 1;
        for (ExportRow item : rows) {
            Row row = sheet.createRow(rowIndex++);
            row.createCell(0).setCellValue(item.getName());
            row.createCell(1).setCellValue(item.getAmount().doubleValue());
            row.createCell(2).setCellValue(item.getCategory());
            row.createCell(3).setCellValue(item.getDate() != null ? item.getDate().toString() : "");
            row.createCell(4).setCellValue(item.getType());
        }
    }

    private void autoSizeColumns(Sheet sheet) {
        for (int i = 0; i < 5; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    @Getter
    @AllArgsConstructor
    private static class ExportRow {
        private String name;
        private BigDecimal amount;
        private String category;
        private LocalDate date;
        private String type;
    }
}
