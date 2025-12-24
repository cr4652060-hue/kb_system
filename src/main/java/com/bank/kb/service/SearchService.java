package com.bank.kb.service;

import com.bank.kb.entity.KnowledgeRecord;
import com.bank.kb.repo.KnowledgeRecordRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SearchService {

    private final KnowledgeRecordRepo repo;

    /**
     * 返回 Page（Controller 已经做了兜底转 List）
     */
    public Page<KnowledgeRecord> search(String q, String category, String department, int limit) {
        String kw = q == null ? "" : q.trim();
        if (kw.isEmpty()) {
            return Page.empty();
        }

        String like = "%" + kw + "%";

        Specification<KnowledgeRecord> spec = (root, query, cb) -> {
            List<Predicate> ands = new ArrayList<>();

            if (category != null && !category.isBlank() && !"全部".equals(category)) {
                ands.add(cb.equal(root.get("category"), category.trim()));
            }
            if (department != null && !department.isBlank()) {
                ands.add(cb.equal(root.get("department"), department.trim()));
            }

            // ✅ 关键：不要对 LONGTEXT/CLOB 做 lower()，直接 like（中文检索足够）
            // ✅ 关键：@Lob 字段显式 as(String.class)，避免 Hibernate/H2 对 CLOB 类型挑剔
            List<Predicate> ors = new ArrayList<>();
            ors.add(cb.like(root.get("bizName"), like)); // varchar，直接 like

            ors.add(cb.like(root.get("process").as(String.class), like));
            ors.add(cb.like(root.get("latestReq").as(String.class), like));
            ors.add(cb.like(root.get("caseText").as(String.class), like));
            ors.add(cb.like(root.get("penalty").as(String.class), like));
            ors.add(cb.like(root.get("basis").as(String.class), like));
            ors.add(cb.like(root.get("keywords").as(String.class), like));

            ands.add(cb.or(ors.toArray(new Predicate[0])));



            return cb.and(ands.toArray(new Predicate[0]));
        };

        // 默认按“最新要求下达时间”倒序（空值放后）
        Sort sort = Sort.by(Sort.Order.desc("latestReqDate"), Sort.Order.desc("id"));
        Pageable pageable = PageRequest.of(0, Math.max(1, limit), sort);

        return repo.findAll(spec, pageable);
    }
}
