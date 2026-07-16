package com.fleet.fleet.service;

import com.fleet.fleet.dto.AuthUserLookupResponse;
import com.fleet.fleet.dto.VehicleAssignmentRequest;
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
import java.util.Optional;
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

    @Mock
    private AuthUserLookupClient authUserLookupClient;

    private VehicleService vehicleService;

    @BeforeEach
    void setUp() {
        vehicleService = new VehicleService(vehicleRepository, imageStorageService, authUserLookupClient);
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

    @Test
    void businessAdminAssignsActiveEmployeeFromOwnOrganization() {
        Vehicle vehicle = vehicle(101L);
        when(vehicleRepository.findById(7L)).thenReturn(Optional.of(vehicle));
        when(authUserLookupClient.lookupUser(20L, "Bearer token")).thenReturn(Optional.of(
                new AuthUserLookupResponse(
                        20L,
                        " driver.one ",
                        "driver@example.com",
                        101L,
                        "EMPLOYEE",
                        "ACTIVE",
                        true
                )
        ));
        when(vehicleRepository.save(vehicle)).thenReturn(vehicle);

        var response = vehicleService.assign(
                7L,
                new VehicleAssignmentRequest(20L),
                businessAdmin(101L),
                "Bearer token"
        );

        assertThat(response.assignedUserId()).isEqualTo(20L);
        assertThat(response.assignedDriverName()).isEqualTo("driver.one");
        assertThat(response.department()).isEqualTo("Operations");
        verify(vehicleRepository).save(vehicle);
    }

    @Test
    void clearingDriverDoesNotCallAuthService() {
        Vehicle vehicle = vehicle(101L);
        vehicle.setAssignedUserId(20L);
        vehicle.setAssignedDriverName("Old Driver");
        when(vehicleRepository.findById(7L)).thenReturn(Optional.of(vehicle));
        when(vehicleRepository.save(vehicle)).thenReturn(vehicle);

        var response = vehicleService.assign(
                7L,
                new VehicleAssignmentRequest(null),
                businessAdmin(101L),
                "Bearer token"
        );

        assertThat(response.assignedUserId()).isNull();
        assertThat(response.assignedDriverName()).isNull();
        verify(authUserLookupClient, never()).lookupUser(any(), any());
    }

    @Test
    void assignmentRejectsEmployeeFromAnotherOrganization() {
        Vehicle vehicle = vehicle(101L);
        when(vehicleRepository.findById(7L)).thenReturn(Optional.of(vehicle));
        when(authUserLookupClient.lookupUser(20L, "Bearer token")).thenReturn(Optional.of(
                new AuthUserLookupResponse(
                        20L,
                        "driver",
                        "driver@example.com",
                        202L,
                        "EMPLOYEE",
                        "ACTIVE",
                        true
                )
        ));

        assertThatThrownBy(() -> vehicleService.assign(
                7L,
                new VehicleAssignmentRequest(20L),
                businessAdmin(101L),
                "Bearer token"
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Selected driver must be an active employee in this organization");

        verify(vehicleRepository, never()).save(any());
    }

    @Test
    void superadminCannotAssignDriver() {
        Vehicle vehicle = vehicle(101L);
        when(vehicleRepository.findById(7L)).thenReturn(Optional.of(vehicle));

        assertThatThrownBy(() -> vehicleService.assign(
                7L,
                new VehicleAssignmentRequest(20L),
                superadmin(),
                "Bearer token"
        )).isInstanceOf(AccessDeniedException.class);

        verify(authUserLookupClient, never()).lookupUser(any(), any());
        verify(vehicleRepository, never()).save(any());
    }

    private Vehicle vehicle(Long businessId) {
        return Vehicle.builder()
                .id(7L)
                .businessId(businessId)
                .licensePlate("B-10-ABC")
                .brand("Dacia")
                .model("Logan")
                .status(VehicleStatus.ACTIVE)
                .department("Operations")
                .currentMileage(10_000L)
                .build();
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
                10_000L
        );
    }

    private Authentication businessAdmin(Long businessId) {
        return authentication("admin", 10L, businessId, "BUSINESS_ADMIN");
    }

    private Authentication employee(Long businessId) {
        return authentication("employee", 20L, businessId, "EMPLOYEE");
    }

    private Authentication superadmin() {
        return authentication("superadmin", 1L, null, "SUPERADMIN");
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
