package com.fleet.auth.repository;

import com.fleet.auth.entity.UserData;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserDataRepository extends JpaRepository<UserData, Long> {
    Optional<UserData> findByEmail(String email);
    Optional<UserData> findByCredentialUsername(String username);
    boolean existsByEmail(String email);
}
