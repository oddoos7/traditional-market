package com.traditionalmarket.backend.user.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.traditionalmarket.backend.user.domain.UserAccount;
import com.traditionalmarket.backend.user.repository.UserAccountRepository;

@Profile("mysql")
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "app.bootstrap-admin.enabled", havingValue = "true")
public class DeploymentAdminDataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DeploymentAdminDataInitializer.class);

    @Bean
    CommandLineRunner initializeDeploymentAdmin(
            UserAccountRepository userAccountRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.bootstrap-admin.email}") String email,
            @Value("${app.bootstrap-admin.password}") String password,
            @Value("${app.bootstrap-admin.name}") String name
    ) {
        return args -> {
            validate(email, "BOOTSTRAP_ADMIN_EMAIL");
            validate(password, "BOOTSTRAP_ADMIN_PASSWORD");
            validate(name, "BOOTSTRAP_ADMIN_NAME");

            if (userAccountRepository.existsByEmailIgnoreCase(email)) {
                log.info("운영 관리자 계정이 이미 존재하여 초기 생성을 건너뜁니다.");
                return;
            }

            UserAccount admin = UserAccount.createAdmin(
                    email,
                    passwordEncoder.encode(password),
                    name
            );
            userAccountRepository.save(admin);
            log.info("운영 관리자 계정을 최초 생성했습니다.");
        };
    }

    private void validate(String value, String environmentName) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(environmentName + " 환경변수는 필수입니다.");
        }
    }
}
