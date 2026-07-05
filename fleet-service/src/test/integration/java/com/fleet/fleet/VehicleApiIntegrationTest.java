package com.fleet.fleet;

import com.fleet.fleet.entity.FuelType;
import com.fleet.fleet.entity.OwnershipType;
import com.fleet.fleet.entity.Vehicle;
import com.fleet.fleet.entity.VehicleStatus;
import com.fleet.fleet.entity.VehicleType;
import com.fleet.fleet.repository.VehicleRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "security.jwt.secret=01234567890123456789012345678901",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false",
        "vehicle.image.storage.path=${java.io.tmpdir}/fleet-api-integration-images"
})
class VehicleApiIntegrationTest {

    private static final String BEARER = "Bearer ";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private VehicleRepository vehicleRepository;

    @BeforeEach
    void setUp() {
        vehicleRepository.deleteAll();
    }

    @Test
    void businessAdminCanCreateAndReadVehicleFromOwnOrganization() throws Exception {
        String token = token("atlas.admin", 1001L, 101L, "BUSINESS_ADMIN");

        mockMvc.perform(post("/vehicles")
                        .header(HttpHeaders.AUTHORIZATION, BEARER + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "businessId": 999,
                                  "licensePlate": " b-101-atl ",
                                  "vin": "vin101",
                                  "brand": "Dacia",
                                  "model": "Duster",
                                  "manufactureYear": 2022,
                                  "vehicleType": "CAR",
                                  "fuelType": "DIESEL",
                                  "ownershipType": "OWNED",
                                  "currentMileage": 12000
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.businessId").value(101))
                .andExpect(jsonPath("$.licensePlate").value("B-101-ATL"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        mockMvc.perform(get("/vehicles")
                        .header(HttpHeaders.AUTHORIZATION, BEARER + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].licensePlate").value("B-101-ATL"));
    }

    @Test
    void employeeOnlySeesAssignedVehiclesAndCannotCreate() throws Exception {
        vehicleRepository.save(vehicle(101L, 2001L, "B-101-OWN"));
        vehicleRepository.save(vehicle(101L, 2002L, "B-102-OTHER"));
        String token = token("employee", 2001L, 101L, "EMPLOYEE");

        mockMvc.perform(get("/vehicles")
                        .header(HttpHeaders.AUTHORIZATION, BEARER + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].licensePlate").value("B-101-OWN"));

        mockMvc.perform(post("/vehicles")
                        .header(HttpHeaders.AUTHORIZATION, BEARER + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "licensePlate": "B-103-DENIED",
                                  "brand": "Ford",
                                  "model": "Focus"
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedVehicleRequestIsRejected() throws Exception {
        mockMvc.perform(get("/vehicles"))
                .andExpect(status().isUnauthorized());
    }

    private Vehicle vehicle(Long businessId, Long assignedUserId, String licensePlate) {
        return Vehicle.builder()
                .businessId(businessId)
                .licensePlate(licensePlate)
                .vin(licensePlate.replace("-", "") + "VIN")
                .brand("Dacia")
                .model("Logan")
                .manufactureYear(2022)
                .vehicleType(VehicleType.CAR)
                .fuelType(FuelType.PETROL)
                .ownershipType(OwnershipType.OWNED)
                .status(VehicleStatus.ACTIVE)
                .assignedUserId(assignedUserId)
                .currentMileage(10_000L)
                .build();
    }

    private String token(String subject, Long userId, Long businessId, String role) {
        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("role", role);
        claims.put("roles", List.of(role));
        claims.put("userId", userId);
        claims.put("businessId", businessId);
        return Jwts.builder()
                .addClaims(claims)
                .setSubject(subject)
                .setIssuedAt(Date.from(Instant.now()))
                .setExpiration(Date.from(Instant.now().plusSeconds(3600)))
                .signWith(
                        Keys.hmacShaKeyFor("01234567890123456789012345678901".getBytes(StandardCharsets.UTF_8)),
                        SignatureAlgorithm.HS256
                )
                .compact();
    }
}
