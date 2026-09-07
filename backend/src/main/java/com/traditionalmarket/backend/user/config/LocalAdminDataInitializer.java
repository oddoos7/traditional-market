package com.traditionalmarket.backend.user.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.traditionalmarket.backend.user.domain.UserAccount;
import com.traditionalmarket.backend.user.repository.UserAccountRepository;

@Profile("local")
@Configuration
public class LocalAdminDataInitializer {

    @Bean
    CommandLineRunner initializeLocalAdmin(
            UserAccountRepository userAccountRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.local-admin.email}") String email,
            @Value("${app.local-admin.password}") String password,
            @Value("${app.local-admin.name}") String name
    ) {
        return args -> {
            if (userAccountRepository.existsByEmailIgnoreCase(email)) {
                return;
            }

            UserAccount admin = UserAccount.createAdmin(
                    email,
                    passwordEncoder.encode(password),
                    name
            );
            userAccountRepository.save(admin);
        };
    }
}
