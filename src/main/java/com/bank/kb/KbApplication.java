package com.bank.kb;

import com.bank.kb.entity.UserAccount;
import com.bank.kb.repo.UserAccountRepo;
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootApplication
public class KbApplication {

    public static void main(String[] args) {
        SpringApplication.run(KbApplication.class, args);
    }

    @Bean
    CommandLineRunner initAdmin(UserAccountRepo repo, PasswordEncoder encoder) {
        return args -> {
            // 初始化管理员：admin / Admin@12345（第一次登录后建议你改密码：第二版再加“改密”功能）
            repo.findByUsername("admin").orElseGet(() -> repo.save(
                    UserAccount.builder()
                            .username("admin")
                            .passwordHash(encoder.encode("Admin@12345"))
                            .role("ADMIN")
                            .department("科技部")
                            .build()
            ));

            // 初始化一个示例部门账号：xindai / Dept@12345
            repo.findByUsername("xindai").orElseGet(() -> repo.save(
                    UserAccount.builder()
                            .username("xindai")
                            .passwordHash(encoder.encode("Dept@12345"))
                            .role("DEPT")
                            .department("信贷管理部")
                            .build()
            ));
        };
    }
}
