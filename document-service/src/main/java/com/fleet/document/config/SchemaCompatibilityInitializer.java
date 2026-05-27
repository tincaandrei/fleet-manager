package com.fleet.document.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class SchemaCompatibilityInitializer implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        jdbcTemplate.execute("alter table if exists documents drop constraint if exists documents_status_check");
        jdbcTemplate.execute("alter table if exists documents drop constraint if exists documents_document_type_check");
        jdbcTemplate.execute("alter table if exists approved_document_data drop constraint if exists approved_document_data_status_check");
        jdbcTemplate.execute("alter table if exists vehicle_document_attributes drop constraint if exists vehicle_document_attributes_status_check");
    }
}
