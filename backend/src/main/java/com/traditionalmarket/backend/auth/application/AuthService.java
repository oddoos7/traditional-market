package com.traditionalmarket.backend.auth.application;

import java.util.Locale;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.traditionalmarket.backend.auth.dto.request.SignupRequest;
import com.traditionalmarket.backend.auth.dto.response.AuthUserResponse;
import com.traditionalmarket.backend.user.domain.UserAccount;
import com.traditionalmarket.backend.user.exception.DuplicateEmailException;
import com.traditionalmarket.backend.user.repository.UserAccountRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public AuthUserResponse signup(SignupRequest request) {
        String email = normalizeEmail(request.email());
        if (userAccountRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateEmailException(email);
        }

        UserAccount user = UserAccount.create(
                email,
                passwordEncoder.encode(request.password()),
                request.name()
        );

        try {
            return AuthUserResponse.from(userAccountRepository.saveAndFlush(user));
        } catch (DataIntegrityViolationException exception) {
            throw new DuplicateEmailException(email);
        }
    }

    public AuthUserResponse findByEmail(String email) {
        UserAccount user = userAccountRepository.findByEmailIgnoreCase(normalizeEmail(email))
                .orElseThrow(() -> new UsernameNotFoundException("회원을 찾을 수 없습니다."));

        return AuthUserResponse.from(user);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
