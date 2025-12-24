package com.bank.kb.config;

import com.bank.kb.entity.UserAccount;
import com.bank.kb.repo.UserAccountRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
public class AdminInit {

    private final UserAccountRepo repo;
    private final PasswordEncoder encoder;

    @Bean
    public CommandLineRunner seedAdmin() {
        return args -> {
            String username = "admin";
            String rawPwd = "Admin@123456"; // 先用这个登录，后面再改

            repo.findByUsername(username).ifPresentOrElse(u -> {
                // 已存在就不动
            }, () -> {
                UserAccount u = new UserAccount();
                u.setUsername(username);
                u.setRole("ADMIN");
                u.setDepartment("科技部");
                u.setPasswordHash(encoder.encode(rawPwd));
                repo.save(u);
                System.out.println("✅ 已创建管理员 admin / Admin@123456");
            });
        };
    }
}
