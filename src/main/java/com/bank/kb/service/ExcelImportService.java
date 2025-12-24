package com.bank.kb.service;

import com.bank.kb.config.DeptCategoryMapping;
import com.bank.kb.entity.KnowledgeRecord;
import com.bank.kb.repo.KnowledgeRecordRepo;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ExcelImportService {

    private final KnowledgeRecordRepo repo;

    // 用于“提醒/校验”，真正匹配表头时会 normalize
    private static final List<String> HEADERS = List.of(
            "分类", "部门", "业务名称", "办理流程", "最新要求下达时间",
            "最新要求", "案例", "扣罚标准", "制度依据", "关键词",
            "维护人", "更新时间", "状态"
    );

    public record ImportResult(int inserted, int skipped, List<String> warnings) {}

    public ImportResult importExcel(MultipartFile file, String uploaderDept) {
        int inserted = 0;
        int skipped = 0;
        List<String> warnings = new ArrayList<>();

        try (InputStream in = file.getInputStream();
             Workbook wb = new XSSFWorkbook(in)) {

            DataFormatter fmt = new DataFormatter();

            for (int s = 0; s < wb.getNumberOfSheets(); s++) {
                Sheet sheet = wb.getSheetAt(s);
                if (sheet == null) continue;

                // ✅ 找表头行：扫描前 30 行，找到包含“业务名称”的那一行
                int headerRowIndex = findHeaderRowIndex(sheet, fmt);
                if (headerRowIndex < 0) {
                    warnings.add("Sheet【" + sheet.getSheetName() + "】未找到表头行（需要包含“业务名称”），已跳过。");
                    continue;
                }
                Row headerRow = sheet.getRow(headerRowIndex);
                if (headerRow == null) {
                    warnings.add("Sheet【" + sheet.getSheetName() + "】表头行为空，已跳过。");
                    continue;
                }

                // ✅ 建立“表头 -> 列号”映射（支持：业务名称（必填）/ 关键词* / 最新要求下达时间(可空) 等）
                Map<String, Integer> col = new HashMap<>();
                for (int c = headerRow.getFirstCellNum(); c < headerRow.getLastCellNum(); c++) {
                    if (c < 0) continue;
                    Cell cell = headerRow.getCell(c);
                    if (cell == null) continue;

                    String name = fmt.formatCellValue(cell).trim();
                    if (name.isBlank()) continue;

                    String normalized = normalizeHeader(name);
                    if (!normalized.isBlank()) {
                        col.put(normalized, c);
                    }
                }

                // 最少必须有“业务名称”
                if (!col.containsKey("业务名称")) {
                    warnings.add("Sheet【" + sheet.getSheetName() + "】缺少表头“业务名称”，已跳过。");
                    continue;
                }

                // 你模板里如果“关键词”是必填，也可以强校验：
                // if (!col.containsKey("关键词")) warnings.add("Sheet【...】缺少表头“关键词”，将按空导入。");

                int last = sheet.getLastRowNum();
                int dataStart = headerRowIndex + 1;

                for (int r = dataStart; r <= last; r++) {
                    Row row = sheet.getRow(r);
                    if (row == null) continue;

                    // ✅ 判空：整行都空就跳过（避免尾部空行算 skipped）
                    if (isRowBlank(row, fmt)) continue;

                    String bizName = v(row, col, "业务名称", fmt);
                    if (bizName.isBlank()) { skipped++; continue; }

                    String dept = v(row, col, "部门", fmt);
                    if (dept.isBlank()) dept = (uploaderDept != null ? uploaderDept : "");

                    String category = v(row, col, "分类", fmt);
                    if (category.isBlank()) category = DeptCategoryMapping.categoryOf(dept);

                    KnowledgeRecord rec = KnowledgeRecord.builder()
                            .category(category)
                            .department(dept)
                            .bizName(bizName)
                            .process(v(row, col, "办理流程", fmt))
                            .latestReqDate(parseDate(v(row, col, "最新要求下达时间", fmt)))
                            .latestReq(v(row, col, "最新要求", fmt))
                            .caseText(v(row, col, "案例", fmt))
                            .penalty(v(row, col, "扣罚标准", fmt))
                            .basis(v(row, col, "制度依据", fmt))
                            .keywords(v(row, col, "关键词", fmt))
                            .owner(v(row, col, "维护人", fmt))
                            .updateTime(v(row, col, "更新时间", fmt))
                            .status(blankToDefault(v(row, col, "状态", fmt), "有效"))
                            .sourceFile(file.getOriginalFilename())
                            .sheetName(sheet.getSheetName())
                            .rowNo(r + 1) // Excel 直观看的行号（从1开始）
                            .build();

                    repo.save(rec);
                    inserted++;
                }
            }

        } catch (Exception e) {
            throw new RuntimeException("导入失败：" + e.getMessage(), e);
        }

        // 可选：提示缺少哪些列（不阻断导入）
        // warnings.addAll(validateMissingHeaders(col));

        return new ImportResult(inserted, skipped, warnings);
    }

    /**
     * ✅ 扫描前 30 行，找到“业务名称”所在行，作为表头行。
     * 适配：第1行是“填写说明”的模板（合并单元格），真正表头在第2行。
     */
    private static int findHeaderRowIndex(Sheet sheet, DataFormatter fmt) {
        int first = sheet.getFirstRowNum();
        int last = Math.min(sheet.getLastRowNum(), first + 30);

        for (int r = first; r <= last; r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;

            short fc = row.getFirstCellNum();
            short lc = row.getLastCellNum();
            if (fc < 0 || lc < 0) continue;

            for (int c = fc; c < lc; c++) {
                Cell cell = row.getCell(c);
                if (cell == null) continue;

                String normalized = normalizeHeader(fmt.formatCellValue(cell));
                if ("业务名称".equals(normalized)) {
                    return r;
                }
            }
        }
        return -1;
    }

    /**
     * ✅ 表头归一化：
     * - 去掉中英文括号内容：业务名称（必填） -> 业务名称
     * - 去掉星号：关键词* -> 关键词
     * - 去掉空格/换行
     */
    private static String normalizeHeader(String raw) {
        if (raw == null) return "";
        String s = raw.trim();
        if (s.isBlank()) return "";

        s = s.replace("\n", "").replace("\r", "").trim();
        s = s.replaceAll("（.*?）", "");
        s = s.replaceAll("\\(.*?\\)", "");
        s = s.replaceAll("\\*", "");
        s = s.replaceAll("\\s+", "");
        return s.trim();
    }

    private static String v(Row row, Map<String, Integer> col, String header, DataFormatter fmt) {
        Integer idx = col.get(header);
        if (idx == null) return "";
        Cell cell = row.getCell(idx);
        if (cell == null) return "";
        return fmt.formatCellValue(cell).trim();
    }

    private static String blankToDefault(String s, String def) {
        return (s == null || s.isBlank()) ? def : s;
    }

    private static LocalDate parseDate(String s) {
        if (s == null) return null;
        String t = s.trim();
        if (t.isBlank()) return null;

        // 常见：2025-12-16 / 2025/12/16 / 2025.12.16
        t = t.replace('.', '-').replace('/', '-');

        List<DateTimeFormatter> fmts = List.of(
                DateTimeFormatter.ofPattern("yyyy-M-d"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd")
        );

        for (DateTimeFormatter f : fmts) {
            try { return LocalDate.parse(t, f); } catch (Exception ignored) {}
        }
        return null;
    }

    /**
     * 避免尾部空行被算 skipped：只要该行所有单元格格式化后都是空，就认为空行。
     */
    private static boolean isRowBlank(Row row, DataFormatter fmt) {
        if (row == null) return true;
        short fc = row.getFirstCellNum();
        short lc = row.getLastCellNum();
        if (fc < 0 || lc < 0) return true;

        for (int c = fc; c < lc; c++) {
            Cell cell = row.getCell(c);
            if (cell == null) continue;
            String v = fmt.formatCellValue(cell);
            if (v != null && !v.trim().isBlank()) return false;
        }
        return true;
    }

    // 可选：提示缺列（不阻断）
    @SuppressWarnings("unused")
    private static List<String> validateMissingHeaders(Map<String, Integer> col) {
        List<String> missing = new ArrayList<>();
        for (String h : HEADERS) {
            if (!col.containsKey(h)) missing.add(h);
        }
        if (!missing.isEmpty()) {
            return List.of("表头缺失列（可选列不影响导入）： " + String.join("、", missing));
        }
        return List.of();
    }
}
