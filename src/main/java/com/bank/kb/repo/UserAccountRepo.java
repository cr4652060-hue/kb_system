package com.bank.kb.repo;

import com.bank.kb.entity.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserAccountRepo extends JpaRepository<UserAccount, Long> {
    Optional<UserAccount> findByUsername(String username);
}
