package com.fleet.auth.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Profile("docker")
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CredentialEmailMigration implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        jdbcTemplate.execute("alter table if exists credentials add column if not exists email varchar(255)");
        jdbcTemplate.execute("""
                update credentials c
                set email = lower(ud.email)
                from user_data ud
                where ud.credential_id = c.credential_id
                  and (c.email is null or c.email = '')
                  and ud.email is not null
                """);
        jdbcTemplate.execute("""
                update credentials
                set email = lower(username)
                where (email is null or email = '')
                  and username like '%@%'
                """);
        jdbcTemplate.execute("""
                update credentials
                set email = lower(username || '@legacy.local')
                where (email is null or email = '')
                  and username is not null
                """);
        jdbcTemplate.execute("alter table if exists credentials alter column username drop not null");
        jdbcTemplate.execute("""
                update credentials
                set username = null
                where username like '%@%'
                """);
        jdbcTemplate.execute("""
                create unique index if not exists uk_credentials_email_lower
                on credentials (lower(email))
                where email is not null
                """);
    }
}
