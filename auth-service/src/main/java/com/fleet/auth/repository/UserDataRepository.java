package com.fleet.auth.repository;

import com.fleet.auth.entity.UserData;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface UserDataRepository extends JpaRepository<UserData, Long> {
    Optional<UserData> findByEmail(String email);
    Optional<UserData> findByCredentialUsername(String username);
    Optional<UserData> findByCredentialEmailIgnoreCase(String email);
    List<UserData> findByUserIdIn(List<Long> userIds);
    List<UserData> findByBusinessIdOrderByCredentialUsernameAsc(Long businessId);
    List<UserData> findByBusinessIdOrderByCredentialEmailAsc(Long businessId);
    List<UserData> findByBusinessIsNullOrderByCredentialUsernameAsc();
    List<UserData> findByBusinessIsNullOrderByCredentialEmailAsc();
    boolean existsByEmail(String email);
}
