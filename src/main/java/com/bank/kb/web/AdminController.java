package com.bank.kb.web;

import com.bank.kb.config.DeptCategoryMapping;
import com.bank.kb.entity.UserAccount;
import com.bank.kb.repo.UserAccountRepo;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.Year;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
public class AdminController {

    private final UserAccountRepo repo;
    private final PasswordEncoder encoder;

    // ========== DTO ==========
    public record CreateUserReq(
            @NotBlank String username,
            @NotBlank String password,
            @NotBlank String role,       // ADMIN / DEPT / USER
            String department
    ) {}

    public record ResetPwdReq(
            @NotBlank String username,
            @NotBlank String newPassword
    ) {}

    public record UserView(
            Long id,
            String username,
            String role,
            String department
    ) {}

    // ========== 基础接口：创建用户 ==========
    @PostMapping("/users")
    public Map<String, Object> createUser(@RequestBody CreateUserReq req) {
        String username = req.username().trim();
        if (repo.findByUsername(username).isPresent()) {
            return Map.of("ok", false, "msg", "用户名已存在：" + username);
        }
        UserAccount u = UserAccount.builder()
                .username(username)
                .passwordHash(encoder.encode(req.password()))
                .role(req.role().trim().toUpperCase())
                .department(req.department() == null ? "" : req.department().trim())
                .build();
        repo.save(u);
        return Map.of("ok", true, "msg", "创建成功：" + username);
    }

    // ========== 管理员：列出所有账号（不含密码） ==========
    @GetMapping("/users")
    public List<UserView> listUsers() {
        return repo.findAll().stream()
                .sorted(Comparator.comparing(UserAccount::getRole).thenComparing(UserAccount::getUsername))
                .map(u -> new UserView(u.getId(), u.getUsername(), u.getRole(), u.getDepartment()))
                .collect(Collectors.toList());
    }

    // ========== 管理员：重置密码 ==========
    @PostMapping("/users/reset-password")
    public Map<String, Object> resetPassword(@RequestBody ResetPwdReq req) {
        String username = req.username().trim();
        var uOpt = repo.findByUsername(username);
        if (uOpt.isEmpty()) return Map.of("ok", false, "msg", "用户不存在：" + username);

        UserAccount u = uOpt.get();
        u.setPasswordHash(encoder.encode(req.newPassword()));
        repo.save(u);
        return Map.of("ok", true, "msg", "已重置密码：" + username);
    }

    // ========== 一键创建全部部门维护账号 ==========
    // 规则：
    // - 用户名：由 deptUsernameMap() 固定映射（你可按习惯修改）
    // - 密码：<首字母大写的用户名>@KB<年份>，例如 Xindai@KB2025
    @PostMapping("/bootstrap-dept-accounts")
    public Map<String, Object> bootstrapDeptAccounts() {
        int year = Year.now().getValue();
        Map<String, String> deptToUser = deptUsernameMap();

        List<Map<String, String>> created = new ArrayList<>();
        List<Map<String, String>> skipped = new ArrayList<>();

        for (var e : deptToUser.entrySet()) {
            String dept = e.getKey();
            String username = e.getValue();

            if (repo.findByUsername(username).isPresent()) {
                skipped.add(Map.of("department", dept, "username", username, "reason", "已存在"));
                continue;
            }

            String password = cap(username) + "@KB" + year;

            UserAccount u = UserAccount.builder()
                    .username(username)
                    .passwordHash(encoder.encode(password))
                    .role("DEPT")
                    .department(dept)
                    .build();
            repo.save(u);

            created.add(Map.of("department", dept, "username", username, "password", password,
                    "category", DeptCategoryMapping.categoryOf(dept)));
        }

        return Map.of(
                "ok", true,
                "year", year,
                "createdCount", created.size(),
                "skippedCount", skipped.size(),
                "created", created,
                "skipped", skipped
        );
    }

    // 你给的部门清单：这里给每个部门配一个“好记的用户名”
    private static Map<String, String> deptUsernameMap() {
        Map<String, String> m = new LinkedHashMap<>();

        // 1. 综合管理类
        m.put("综合部", "zonghe");
        m.put("党委办公室", "dangwei");
        m.put("监事会办公室", "jianshi");
        m.put("农金员办公室", "nongjin");

        // 2. 人事与监督类（✅ 纪检监察部在这里）
        m.put("人力资源部", "renshi");
        m.put("审计部", "shenji");
        m.put("纪检监察部", "jijian");

        // 3. 信贷与风险类
        m.put("信贷管理部", "xindai");
        m.put("风险管理部", "fengxian");
        m.put("贷款营销调查中心", "diaochazhongxin");
        m.put("贷后检查管理中心", "daihou");
        m.put("贷款审查审批中心", "shenpi");

        // 4. 财务与运营类
        m.put("财务会计部", "caiwu");
        m.put("运营管理部", "yunying");
        m.put("资产经营中心", "zichan");

        // 5. 业务与拓展类
        m.put("业务发展部", "yewufazhan");
        m.put("金融市场部", "jinrongshichang");

        // 6. 保障类
        m.put("安全保卫部", "baoan");
        m.put("法律合规部", "hegui");
        m.put("科技部", "keji");

        return m;
    }

    private static String cap(String s) {
        if (s == null || s.isBlank()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
