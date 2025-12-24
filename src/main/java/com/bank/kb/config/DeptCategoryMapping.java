package com.bank.kb.config;

import java.util.*;

public class DeptCategoryMapping {

    public static final List<String> CATEGORIES = List.of(
            "综合管理类", "人事与监督类", "信贷与风险类", "财务与运营类", "业务与拓展类", "保障类"
    );

    public static final Map<String, String> DEPT_TO_CATEGORY;
    static {
        Map<String, String> m = new LinkedHashMap<>();

        // 1. 综合管理类
        m.put("综合部", "综合管理类");
        m.put("党委办公室", "综合管理类");
        m.put("监事会办公室", "综合管理类");
        m.put("农金员办公室", "综合管理类");

        // 2. 人事与监督类
        m.put("人力资源部", "人事与监督类");
        m.put("审计部", "人事与监督类");
        m.put("纪检监察部", "人事与监督类");

        // 3. 信贷与风险类
        m.put("信贷管理部", "信贷与风险类");
        m.put("风险管理部", "信贷与风险类");
        m.put("贷款营销调查中心", "信贷与风险类");
        m.put("贷后检查管理中心", "信贷与风险类");
        m.put("贷款审查审批中心", "信贷与风险类");

        // 4. 财务与运营类
        m.put("财务会计部", "财务与运营类");
        m.put("运营管理部", "财务与运营类");
        m.put("资产经营中心", "财务与运营类");

        // 5. 业务与拓展类
        m.put("业务发展部", "业务与拓展类");
        m.put("金融市场部", "业务与拓展类");

        // 6. 保障类
        m.put("安全保卫部", "保障类");
        m.put("法律合规部", "保障类");
        m.put("科技部", "保障类");

        DEPT_TO_CATEGORY = Collections.unmodifiableMap(m);
    }

    public static String categoryOf(String dept) {
        if (dept == null) return "";
        return DEPT_TO_CATEGORY.getOrDefault(dept.trim(), "未分类");
    }
}
