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
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.endsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "security.jwt.secret=01234567890123456789012345678901",
        "vehicle.image.storage.path=${java.io.tmpdir}/fleet-test-vehicle-images"
})
class VehicleImageSecurityIntegrationTest {

    private static final String BEARER = "Bearer ";
    private static final byte[] PNG_BYTES = {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
    };

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private VehicleRepository vehicleRepository;

    private Vehicle atlasVehicle;
    private Vehicle novaVehicle;

    @BeforeEach
    void setUp() {
        vehicleRepository.deleteAll();
        atlasVehicle = vehicleRepository.save(vehicle(101L, "B-101-ATL"));
        novaVehicle = vehicleRepository.save(vehicle(102L, "CJ-201-NVA"));
    }

    @Test
    void businessAdminCanUploadVehicleImageForOwnBusiness() throws Exception {
        mockMvc.perform(multipart("/vehicles/{id}/image", atlasVehicle.getId())
                        .file(imageFile())
                        .header("Authorization", BEARER + token("atlas.admin", 1001L, 101L, "BUSINESS_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imageUrl").value("/api/fleet/vehicles/%d/image".formatted(atlasVehicle.getId())))
                .andExpect(jsonPath("$.imageOriginalFileName").value("vehicle.png"));
    }

    @Test
    void businessAdminCannotUploadVehicleImageForOtherBusiness() throws Exception {
        mockMvc.perform(multipart("/vehicles/{id}/image", novaVehicle.getId())
                        .file(imageFile())
                        .header("Authorization", BEARER + token("atlas.admin", 1001L, 101L, "BUSINESS_ADMIN")))
                .andExpect(status().isForbidden());
    }

    @Test
    void superadminCanUploadAndDeleteVehicleImageAcrossBusinesses() throws Exception {
        mockMvc.perform(multipart("/vehicles/{id}/image", novaVehicle.getId())
                        .file(imageFile())
                        .header("Authorization", BEARER + token("admin", 1L, null, "SUPERADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imageUrl").value(endsWith("/vehicles/%d/image".formatted(novaVehicle.getId()))));

        mockMvc.perform(delete("/vehicles/{id}/image", novaVehicle.getId())
                        .header("Authorization", BEARER + token("admin", 1L, null, "SUPERADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imageUrl").doesNotExist());
    }

    private MockMultipartFile imageFile() {
        return new MockMultipartFile("file", "vehicle.png", MediaType.IMAGE_PNG_VALUE, PNG_BYTES);
    }

    private Vehicle vehicle(Long businessId, String licensePlate) {
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
                .department("Operations")
                .currentMileage(12000L)
                .build();
    }

    private String token(String subject, Long userId, Long businessId, String role) {
        Map<String, Object> claims = new java.util.LinkedHashMap<>();
        claims.put("role", role);
        claims.put("roles", List.of(role));
        claims.put("userId", userId);
        if (businessId != null) {
            claims.put("businessId", businessId);
        }
        return Jwts.builder()
                .addClaims(claims)
                .setSubject(subject)
                .setIssuedAt(Date.from(Instant.now()))
                .setExpiration(Date.from(Instant.now().plusSeconds(3600)))
                .signWith(Keys.hmacShaKeyFor("01234567890123456789012345678901".getBytes(StandardCharsets.UTF_8)),
                        SignatureAlgorithm.HS256)
                .compact();
    }
}
