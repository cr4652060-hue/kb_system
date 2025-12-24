package com.bank.kb.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_account")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 60)
    private String username;

    @Column(nullable = false, length = 200)
    private String passwordHash; // BCrypt

    @Column(nullable = false, length = 30)
    private String role; // ADMIN / DEPT / USER

    @Column(length = 60)
    private String department; // 部门账号所属部门（DEPT用）
}
