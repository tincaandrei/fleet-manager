package com.fleet.fleet.service;

import com.fleet.fleet.dto.VehicleAssignmentRequest;
import com.fleet.fleet.dto.VehicleBasicInfoResponse;
import com.fleet.fleet.dto.VehicleExistsResponse;
import com.fleet.fleet.dto.VehicleRequest;
import com.fleet.fleet.dto.VehicleResponse;
import com.fleet.fleet.entity.FuelType;
import com.fleet.fleet.entity.OwnershipType;
import com.fleet.fleet.entity.Vehicle;
import com.fleet.fleet.entity.VehicleStatus;
import com.fleet.fleet.entity.VehicleType;
import com.fleet.fleet.exception.DuplicateVehicleException;
import com.fleet.fleet.exception.ResourceNotFoundException;
import com.fleet.fleet.repository.VehicleRepository;
import com.fleet.fleet.repository.VehicleSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class VehicleService {

    private final VehicleRepository vehicleRepository;

    @Transactional
    public VehicleResponse create(VehicleRequest request, Authentication authentication) {
        Long businessId = resolveWritableBusinessId(request.businessId(), authentication);
        String licensePlate = normalizeUpper(request.licensePlate());
        String vin = normalizeOptionalUpper(request.vin());
        validateUniqueVehicle(null, businessId, licensePlate, vin);

        Vehicle vehicle = Vehicle.builder()
                .businessId(businessId)
                .licensePlate(licensePlate)
                .vin(vin)
                .brand(normalizeRequired(request.brand()))
                .model(normalizeRequired(request.model()))
                .manufactureYear(request.manufactureYear())
                .vehicleType(request.vehicleType())
                .fuelType(request.fuelType())
                .ownershipType(request.ownershipType())
                .status(request.status() == null ? VehicleStatus.ACTIVE : request.status())
                .department(normalizeOptional(request.department()))
                .assignedUserId(request.assignedUserId())
                .assignedDriverName(normalizeOptional(request.assignedDriverName()))
                .currentMileage(request.currentMileage())
                .build();
        return toResponse(vehicleRepository.save(vehicle));
    }

    @Transactional(readOnly = true)
    public List<VehicleResponse> findAll(
            VehicleStatus status,
            VehicleType vehicleType,
            FuelType fuelType,
            OwnershipType ownershipType,
            String department,
            Long assignedUserId,
            String licensePlate,
            Long businessIdFilter,
            Authentication authentication
    ) {
        Long effectiveBusinessId = resolveReadableBusinessId(businessIdFilter, authentication);
        Specification<Vehicle> specification = VehicleSpecifications.filterBy(
                status,
                effectiveBusinessId,
                vehicleType,
                fuelType,
                ownershipType,
                department,
                assignedUserId,
                licensePlate
        );

        if (SecurityUtils.isEmployeeOnly(authentication)) {
            Long currentUserId = SecurityUtils.currentUserId(authentication);
            if (currentUserId == null) {
                return List.of();
            }
            specification = specification.and(VehicleSpecifications.assignedTo(currentUserId));
        }

        return vehicleRepository.findAll(specification, Sort.by(Sort.Direction.ASC, "licensePlate")).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public VehicleResponse getById(Long id, Authentication authentication) {
        Vehicle vehicle = getVehicle(id);
        assertCanRead(vehicle, authentication);
        return toResponse(vehicle);
    }

    @Transactional
    public VehicleResponse update(Long id, VehicleRequest request, Authentication authentication) {
        Vehicle vehicle = getVehicle(id);
        assertCanWrite(vehicle, authentication);
        String licensePlate = normalizeUpper(request.licensePlate());
        String vin = normalizeOptionalUpper(request.vin());
        validateUniqueVehicle(vehicle, vehicle.getBusinessId(), licensePlate, vin);

        vehicle.setLicensePlate(licensePlate);
        vehicle.setVin(vin);
        vehicle.setBrand(normalizeRequired(request.brand()));
        vehicle.setModel(normalizeRequired(request.model()));
        vehicle.setManufactureYear(request.manufactureYear());
        vehicle.setVehicleType(request.vehicleType());
        vehicle.setFuelType(request.fuelType());
        vehicle.setOwnershipType(request.ownershipType());
        vehicle.setStatus(request.status() == null ? VehicleStatus.ACTIVE : request.status());
        vehicle.setDepartment(normalizeOptional(request.department()));
        vehicle.setAssignedUserId(request.assignedUserId());
        vehicle.setAssignedDriverName(normalizeOptional(request.assignedDriverName()));
        vehicle.setCurrentMileage(request.currentMileage());
        return toResponse(vehicleRepository.save(vehicle));
    }

    @Transactional
    public VehicleResponse changeStatus(Long id, VehicleStatus status, Authentication authentication) {
        Vehicle vehicle = getVehicle(id);
        assertCanWrite(vehicle, authentication);
        vehicle.setStatus(status);
        return toResponse(vehicleRepository.save(vehicle));
    }

    @Transactional
    public VehicleResponse assign(Long id, VehicleAssignmentRequest request, Authentication authentication) {
        Vehicle vehicle = getVehicle(id);
        assertCanWrite(vehicle, authentication);
        vehicle.setAssignedUserId(request.assignedUserId());
        vehicle.setAssignedDriverName(normalizeOptional(request.assignedDriverName()));
        vehicle.setDepartment(normalizeOptional(request.department()));
        return toResponse(vehicleRepository.save(vehicle));
    }

    @Transactional
    public void delete(Long id, Authentication authentication) {
        Vehicle vehicle = getVehicle(id);
        assertCanWrite(vehicle, authentication);
        vehicleRepository.delete(vehicle);
    }

    @Transactional(readOnly = true)
    public VehicleExistsResponse exists(Long id, Authentication authentication) {
        return vehicleRepository.findById(id)
                .filter(vehicle -> canRead(vehicle, authentication))
                .map(vehicle -> new VehicleExistsResponse(id, vehicle.getBusinessId(), true))
                .orElse(new VehicleExistsResponse(id, null, false));
    }

    @Transactional(readOnly = true)
    public VehicleBasicInfoResponse basicInfo(Long id, Authentication authentication) {
        Vehicle vehicle = getVehicle(id);
        assertCanRead(vehicle, authentication);
        return toBasicInfo(vehicle);
    }

    @Transactional(readOnly = true)
    public List<VehicleBasicInfoResponse> activeVehicles(Long businessIdFilter, Authentication authentication) {
        Long effectiveBusinessId = resolveReadableBusinessId(businessIdFilter, authentication);
        List<Vehicle> vehicles = effectiveBusinessId == null
                ? vehicleRepository.findByStatus(VehicleStatus.ACTIVE)
                : vehicleRepository.findByBusinessIdAndStatus(effectiveBusinessId, VehicleStatus.ACTIVE);
        return vehicles.stream()
                .filter(vehicle -> canRead(vehicle, authentication))
                .map(this::toBasicInfo)
                .toList();
    }

    private Vehicle getVehicle(Long id) {
        return vehicleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found: " + id));
    }

    private void assertCanRead(Vehicle vehicle, Authentication authentication) {
        if (canRead(vehicle, authentication)) {
            return;
        }
        throw new AccessDeniedException("Access denied");
    }

    private boolean canRead(Vehicle vehicle, Authentication authentication) {
        if (SecurityUtils.isSuperadmin(authentication)) {
            return true;
        }
        Long businessId = SecurityUtils.currentBusinessId(authentication);
        if (businessId == null || !businessId.equals(vehicle.getBusinessId())) {
            return false;
        }
        if (SecurityUtils.isBusinessAdmin(authentication)) {
            return true;
        }
        Long currentUserId = SecurityUtils.currentUserId(authentication);
        return currentUserId != null && currentUserId.equals(vehicle.getAssignedUserId());
    }

    private void assertCanWrite(Vehicle vehicle, Authentication authentication) {
        if (SecurityUtils.isSuperadmin(authentication)) {
            return;
        }
        if (SecurityUtils.isBusinessAdmin(authentication)
                && SecurityUtils.currentBusinessId(authentication) != null
                && SecurityUtils.currentBusinessId(authentication).equals(vehicle.getBusinessId())) {
            return;
        }
        throw new AccessDeniedException("Access denied");
    }

    private Long resolveWritableBusinessId(Long requestedBusinessId, Authentication authentication) {
        if (SecurityUtils.isSuperadmin(authentication)) {
            if (requestedBusinessId == null) {
                throw new IllegalArgumentException("Business id is required for SUPERADMIN vehicle creation");
            }
            return requestedBusinessId;
        }
        if (SecurityUtils.isBusinessAdmin(authentication) && SecurityUtils.currentBusinessId(authentication) != null) {
            return SecurityUtils.currentBusinessId(authentication);
        }
        throw new AccessDeniedException("Access denied");
    }

    private Long resolveReadableBusinessId(Long requestedBusinessId, Authentication authentication) {
        if (SecurityUtils.isSuperadmin(authentication)) {
            return requestedBusinessId;
        }
        Long businessId = SecurityUtils.currentBusinessId(authentication);
        if (businessId == null) {
            throw new AccessDeniedException("Access denied");
        }
        return businessId;
    }

    private void validateUniqueVehicle(Vehicle currentVehicle, Long businessId, String licensePlate, String vin) {
        boolean plateChanged = currentVehicle == null
                || !currentVehicle.getLicensePlate().equalsIgnoreCase(licensePlate);
        if (plateChanged && vehicleRepository.existsByBusinessIdAndLicensePlateIgnoreCase(businessId, licensePlate)) {
            throw new DuplicateVehicleException("License plate is already registered");
        }

        boolean hasVin = StringUtils.hasText(vin);
        boolean vinChanged = currentVehicle == null
                || currentVehicle.getVin() == null
                || !currentVehicle.getVin().equalsIgnoreCase(vin);
        if (hasVin && vinChanged && vehicleRepository.existsByBusinessIdAndVinIgnoreCase(businessId, vin)) {
            throw new DuplicateVehicleException("VIN is already registered");
        }
    }

    private VehicleResponse toResponse(Vehicle vehicle) {
        return new VehicleResponse(
                vehicle.getId(),
                vehicle.getBusinessId(),
                vehicle.getLicensePlate(),
                vehicle.getVin(),
                vehicle.getBrand(),
                vehicle.getModel(),
                vehicle.getManufactureYear(),
                vehicle.getVehicleType(),
                vehicle.getFuelType(),
                vehicle.getOwnershipType(),
                vehicle.getStatus(),
                vehicle.getDepartment(),
                vehicle.getAssignedUserId(),
                vehicle.getAssignedDriverName(),
                vehicle.getCurrentMileage(),
                vehicle.getCreatedAt(),
                vehicle.getUpdatedAt()
        );
    }

    private VehicleBasicInfoResponse toBasicInfo(Vehicle vehicle) {
        return new VehicleBasicInfoResponse(
                vehicle.getId(),
                vehicle.getBusinessId(),
                vehicle.getLicensePlate(),
                vehicle.getBrand(),
                vehicle.getModel(),
                vehicle.getStatus(),
                vehicle.getAssignedUserId(),
                vehicle.getAssignedDriverName()
        );
    }

    private String normalizeUpper(String value) {
        return normalizeRequired(value).toUpperCase(Locale.ROOT);
    }

    private String normalizeOptionalUpper(String value) {
        String normalized = normalizeOptional(value);
        return normalized == null ? null : normalized.toUpperCase(Locale.ROOT);
    }

    private String normalizeRequired(String value) {
        return value.trim();
    }

    private String normalizeOptional(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
