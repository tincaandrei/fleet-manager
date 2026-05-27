package com.fleet.auth.repository;

import com.fleet.auth.entity.Business;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BusinessRepository extends JpaRepository<Business, Long> {
    boolean existsByNameIgnoreCase(String name);
}
