package com.traditionalmarket.backend.user.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.traditionalmarket.backend.user.domain.UserAccount;
import com.traditionalmarket.backend.user.domain.UserRole;
import com.traditionalmarket.backend.user.repository.UserAccountRepository;

@ExtendWith(MockitoExtension.class)
class DeploymentAdminDataInitializerTest {

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private final DeploymentAdminDataInitializer initializer =
            new DeploymentAdminDataInitializer();

    @Test
    void createsAdminWhenEnabledConfigurationIsValid() throws Exception {
        when(userAccountRepository.existsByEmailIgnoreCase("Admin@Example.com"))
                .thenReturn(false);
        when(passwordEncoder.encode("strong-password"))
                .thenReturn("encoded-password");

        CommandLineRunner runner = initializer.initializeDeploymentAdmin(
                userAccountRepository,
                passwordEncoder,
                "Admin@Example.com",
                "strong-password",
                "시장 관리자"
        );

        runner.run();

        ArgumentCaptor<UserAccount> accountCaptor = ArgumentCaptor.forClass(UserAccount.class);
        verify(userAccountRepository).save(accountCaptor.capture());
        assertEquals("admin@example.com", accountCaptor.getValue().getEmail());
        assertEquals("encoded-password", accountCaptor.getValue().getPasswordHash());
        assertEquals(UserRole.ADMIN, accountCaptor.getValue().getRole());
    }

    @Test
    void doesNotOverwriteExistingAccount() throws Exception {
        when(userAccountRepository.existsByEmailIgnoreCase("admin@example.com"))
                .thenReturn(true);

        CommandLineRunner runner = initializer.initializeDeploymentAdmin(
                userAccountRepository,
                passwordEncoder,
                "admin@example.com",
                "strong-password",
                "시장 관리자"
        );

        runner.run();

        verify(passwordEncoder, never()).encode("strong-password");
        verify(userAccountRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void rejectsBlankRequiredConfiguration() {
        CommandLineRunner runner = initializer.initializeDeploymentAdmin(
                userAccountRepository,
                passwordEncoder,
                "",
                "strong-password",
                "시장 관리자"
        );

        assertThrows(IllegalStateException.class, runner::run);
    }
}
