package com.fleet.auth.repository;

import com.fleet.auth.entity.InvitationToken;
import com.fleet.auth.entity.UserData;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InvitationTokenRepository extends JpaRepository<InvitationToken, UUID> {

    Optional<InvitationToken> findByTokenHash(String tokenHash);

    List<InvitationToken> findByUserAndUsedAtIsNull(UserData user);
}
