package com.bank.kb.web;

import com.bank.kb.entity.UserAccount;
import com.bank.kb.repo.UserAccountRepo;
import lombok.RequiredArgsConstructor;
import org.apache.catalina.User;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AuthController {

    private final UserAccountRepo userRepo;


    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> me(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "not logged in");
        }

        String username = auth.getName();
        UserAccount u = userRepo.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "user not found"));

        Map<String,Object> body = Map.of(
                "username", u.getUsername(),
                "role", u.getRole() == null ? "" : u.getRole(),
                "department", u.getDepartment() == null ? "" : u.getDepartment()
        );

        return ResponseEntity.ok()
                .header("Cache-Control", "no-store, no-cache, max-age=0, must-revalidate")
                .header("Pragma", "no-cache")
                .body(body);
    }



}
