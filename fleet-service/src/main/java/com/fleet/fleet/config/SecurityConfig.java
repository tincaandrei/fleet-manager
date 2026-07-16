package com.fleet.fleet.config;

import com.fleet.fleet.filter.JwtAuthFilter;
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
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableConfigurationProperties({JwtProperties.class, VehicleImageStorageProperties.class})
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthFilter jwtAuthFilter,
            CorsConfigurationSource corsConfigurationSource
    ) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/v3/api-docs.yaml").permitAll()
                        .requestMatchers("/internal/vehicles/**").hasAnyRole("SUPERADMIN", "BUSINESS_ADMIN", "EMPLOYEE")
                        .requestMatchers(HttpMethod.GET, "/vehicles", "/vehicles/**").hasAnyRole("SUPERADMIN", "BUSINESS_ADMIN", "EMPLOYEE")
                        .requestMatchers(HttpMethod.POST, "/vehicles").hasAnyRole("SUPERADMIN", "BUSINESS_ADMIN")
                        .requestMatchers(HttpMethod.POST, "/vehicles/*/image").hasAnyRole("SUPERADMIN", "BUSINESS_ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/vehicles/*/image").hasAnyRole("SUPERADMIN", "BUSINESS_ADMIN")
                        .requestMatchers(HttpMethod.POST, "/vehicles/**").hasAnyRole("SUPERADMIN", "BUSINESS_ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/vehicles/**").hasAnyRole("SUPERADMIN", "BUSINESS_ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/vehicles/*/assignment").hasRole("BUSINESS_ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/vehicles/**").hasAnyRole("SUPERADMIN", "BUSINESS_ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/vehicles/**").hasAnyRole("SUPERADMIN", "BUSINESS_ADMIN")
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

    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @org.springframework.beans.factory.annotation.Value("${app.cors.allowed-origins}") String allowedOrigins
    ) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isBlank())
                .toList());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Requested-With"));
        configuration.setExposedHeaders(List.of("Content-Disposition"));
        configuration.setAllowCredentials(false);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
