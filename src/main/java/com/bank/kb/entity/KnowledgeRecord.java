package com.bank.kb.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "knowledge_record",
        indexes = {
                @Index(name="idx_kb_category", columnList="category"),
                @Index(name="idx_kb_dept", columnList="department"),
                @Index(name="idx_kb_latestDate", columnList="latestReqDate")
        })
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KnowledgeRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 分类（综合管理类/信贷与风险类…）
    @Column(length = 30)
    private String category;

    // 部门
    @Column(length = 60)
    private String department;

    // 业务名称
    @Column(length = 200)
    private String bizName;

    // 办理流程
    @Lob
    private String process;

    // 最新要求下达时间
    private LocalDate latestReqDate;

    // 最新要求
    @Lob
    private String latestReq;

    // 案例
    @Lob
    private String caseText;

    // 扣罚标准
    @Lob
    private String penalty;

    // 制度依据
    @Lob
    private String basis;

    // 关键词（同义词、简称）
    @Lob
    private String keywords;

    // 维护人
    @Column(length = 60)
    private String owner;

    // 更新时间（部门给的）
    @Column(length = 30)
    private String updateTime;

    // 状态：有效/作废
    @Column(length = 10)
    private String status;

    // 来源定位（文件名/Sheet/行号）
    @Column(length = 200)
    private String sourceFile;

    @Column(length = 100)
    private String sheetName;

    private Integer rowNo;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
        if (status == null || status.isBlank()) status = "有效";
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
