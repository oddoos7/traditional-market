package com.traditionalmarket.backend.user.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.traditionalmarket.backend.user.domain.UserAccount;

import jakarta.persistence.LockModeType;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {

    Optional<UserAccount> findByEmailIgnoreCase(String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM UserAccount u WHERE LOWER(u.email) = LOWER(:email)")
    Optional<UserAccount> findByEmailIgnoreCaseForUpdate(@Param("email") String email);

    boolean existsByEmailIgnoreCase(String email);
}
