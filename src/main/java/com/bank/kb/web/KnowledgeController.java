package com.bank.kb.web;

import com.bank.kb.entity.KnowledgeRecord;
import com.bank.kb.repo.KnowledgeRecordRepo;
import com.bank.kb.service.ExcelImportService;
import com.bank.kb.service.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;


@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class KnowledgeController {

    private final SearchService searchService;
    private final KnowledgeRecordRepo repo;
    private final ExcelImportService excelImportService;

    /**
     * 🔍 搜索（前端主用）
     * GET /api/search?q=门&category=保障类&department=科技部&limit=200
     *
     * 注意：SearchService 返回 Page，这里必须转 List，否则就会“爆红”
     */
    @GetMapping("/search")
    public List<KnowledgeRecord> search(
            @RequestParam("q") String q,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "department", required = false) String department,
            @RequestParam(value = "limit", defaultValue = "200") int limit
    ) {
        int size = Math.min(Math.max(limit, 1), 200);
        Page<KnowledgeRecord> page = searchService.search(q, category, department, size);
        return page.getContent();
    }

    /**
     * 兼容旧路径（如果你前端/历史代码还在用）
     * GET /api/knowledge/search?q=门
     */
    @GetMapping("/knowledge/search")
    public List<KnowledgeRecord> searchCompat(
            @RequestParam("q") String q,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "department", required = false) String department,
            @RequestParam(value = "limit", defaultValue = "200") int limit
    ) {
        int size = Math.min(Math.max(limit, 1), 200);
        Page<KnowledgeRecord> page = searchService.search(q, category, department, size);
        return page.getContent();
    }

    /**
     * 📄 列表（调试用）
     * GET /api/knowledge?limit=50
     */
    @GetMapping("/knowledge")
    public List<KnowledgeRecord> list(
            @RequestParam(value = "limit", defaultValue = "50") int limit
    ) {
        int size = Math.min(Math.max(limit, 1), 200);
        return repo.findAll(PageRequest.of(0, size)).getContent();
    }

    /**
     * ➕ 新增单条（给“新增知识”弹窗用）
     * POST /api/knowledge
     * Content-Type: application/json
     */
    @PostMapping("/knowledge")
    public KnowledgeRecord add(@RequestBody KnowledgeRecord rec, Authentication auth) {

        // 必填校验（你也可以按你们模板调整）
        if (rec.getBizName() == null || rec.getBizName().isBlank()) {
            throw new IllegalArgumentException("业务名称不能为空");
        }
        if (rec.getKeywords() == null || rec.getKeywords().isBlank()) {
            throw new IllegalArgumentException("关键词不能为空");
        }

        // 默认字段兜底
        if (auth != null && auth.isAuthenticated()) {
            rec.setOwner(auth.getName());
        }
        if (rec.getStatus() == null || rec.getStatus().isBlank()) {
            rec.setStatus("有效");
        }
        if (rec.getUpdateTime() == null || rec.getUpdateTime().isBlank()) {
            rec.setUpdateTime(LocalDate.now().toString());
        }

        return repo.save(rec);
    }

    /**
     * 📥 Excel 导入（王行长要的主入口）
     * POST /api/knowledge/import (form-data: file)
     */
    @PostMapping("/knowledge/import")
    public ExcelImportService.ImportResult importExcel(@RequestParam("file") MultipartFile file) {
        // uploaderDept 如果前端能传，就改成 @RequestParam(required=false) String dept
        return excelImportService.importExcel(file, null);
    }
}
