package com.fleet.fleet.service;

import com.fleet.fleet.dto.VehicleRequest;
import com.fleet.fleet.entity.FuelType;
import com.fleet.fleet.entity.OwnershipType;
import com.fleet.fleet.entity.Vehicle;
import com.fleet.fleet.entity.VehicleStatus;
import com.fleet.fleet.entity.VehicleType;
import com.fleet.fleet.exception.DuplicateVehicleException;
import com.fleet.fleet.repository.VehicleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VehicleServiceTest {

    @Mock
    private VehicleRepository vehicleRepository;

    @Mock
    private VehicleImageStorageService imageStorageService;

    private VehicleService vehicleService;

    @BeforeEach
    void setUp() {
        vehicleService = new VehicleService(vehicleRepository, imageStorageService);
    }

    @Test
    void businessAdminCreatesVehicleInOwnOrganizationAndNormalizesData() {
        when(vehicleRepository.save(any(Vehicle.class))).thenAnswer(invocation -> {
            Vehicle vehicle = invocation.getArgument(0);
            vehicle.setId(10L);
            return vehicle;
        });

        var response = vehicleService.create(request(999L, " b-10-abc ", " vin-10 "), businessAdmin(101L));

        assertThat(response.businessId()).isEqualTo(101L);
        assertThat(response.licensePlate()).isEqualTo("B-10-ABC");
        assertThat(response.vin()).isEqualTo("VIN-10");
        assertThat(response.status()).isEqualTo(VehicleStatus.ACTIVE);
        verify(vehicleRepository).save(any(Vehicle.class));
    }

    @Test
    void duplicateLicensePlateIsRejectedBeforeSaving() {
        when(vehicleRepository.existsByBusinessIdAndLicensePlateIgnoreCase(101L, "B-10-ABC"))
                .thenReturn(true);

        assertThatThrownBy(() -> vehicleService.create(
                request(null, "B-10-ABC", "VIN-10"),
                businessAdmin(101L)
        )).isInstanceOf(DuplicateVehicleException.class)
                .hasMessage("License plate is already registered");

        verify(vehicleRepository, never()).save(any());
    }

    @Test
    void employeeCannotCreateVehicle() {
        assertThatThrownBy(() -> vehicleService.create(
                request(101L, "B-10-ABC", "VIN-10"),
                employee(101L)
        )).isInstanceOf(AccessDeniedException.class);

        verify(vehicleRepository, never()).save(any());
    }

    private VehicleRequest request(Long businessId, String licensePlate, String vin) {
        return new VehicleRequest(
                businessId,
                licensePlate,
                vin,
                " Dacia ",
                " Logan ",
                2022,
                VehicleType.CAR,
                FuelType.PETROL,
                OwnershipType.OWNED,
                null,
                " Operations ",
                null,
                null,
                10_000L
        );
    }

    private Authentication businessAdmin(Long businessId) {
        return authentication("admin", 10L, businessId, "BUSINESS_ADMIN");
    }

    private Authentication employee(Long businessId) {
        return authentication("employee", 20L, businessId, "EMPLOYEE");
    }

    private Authentication authentication(String username, Long userId, Long businessId, String role) {
        JwtPrincipal principal = new JwtPrincipal(username, userId, businessId, Set.of(role));
        return new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role))
        );
    }
}
