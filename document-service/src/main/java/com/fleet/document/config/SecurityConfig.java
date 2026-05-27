package com.fleet.document.config;

import com.fleet.document.filter.JwtAuthFilter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableConfigurationProperties({JwtProperties.class, DocumentStorageProperties.class})
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthFilter jwtAuthFilter) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/v3/api-docs.yaml").permitAll()
                        .requestMatchers(HttpMethod.GET, "/review-queue").hasAnyRole("SUPERADMIN", "BUSINESS_ADMIN")
                        .requestMatchers(HttpMethod.POST, "/*/review").hasAnyRole("SUPERADMIN", "BUSINESS_ADMIN")
                        .requestMatchers(HttpMethod.POST, "/*/approve").hasAnyRole("SUPERADMIN", "BUSINESS_ADMIN")
                        .requestMatchers(HttpMethod.POST, "/*/reject").hasAnyRole("SUPERADMIN", "BUSINESS_ADMIN")
                        .requestMatchers(HttpMethod.POST, "/*/archive").hasAnyRole("SUPERADMIN", "BUSINESS_ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/*/archive").hasAnyRole("SUPERADMIN", "BUSINESS_ADMIN")
                        .requestMatchers(HttpMethod.POST, "/**").hasAnyRole("SUPERADMIN", "BUSINESS_ADMIN", "EMPLOYEE")
                        .requestMatchers(HttpMethod.GET, "/", "/*", "/*/download").authenticated()
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpStatus.UNAUTHORIZED.value());
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            response.getWriter().write("{\"message\":\"Authentication required\"}");
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(HttpStatus.FORBIDDEN.value());
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            response.getWriter().write("{\"message\":\"Access denied\"}");
                        })
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
