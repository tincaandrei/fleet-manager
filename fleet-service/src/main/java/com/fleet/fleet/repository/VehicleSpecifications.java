package com.fleet.fleet.repository;

import com.fleet.fleet.entity.FuelType;
import com.fleet.fleet.entity.OwnershipType;
import com.fleet.fleet.entity.Vehicle;
import com.fleet.fleet.entity.VehicleStatus;
import com.fleet.fleet.entity.VehicleType;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public final class VehicleSpecifications {

    private VehicleSpecifications() {
    }

    public static Specification<Vehicle> filterBy(
            VehicleStatus status,
            Long businessId,
            VehicleType vehicleType,
            FuelType fuelType,
            OwnershipType ownershipType,
            String department,
            Long assignedUserId,
            String licensePlate
    ) {
        return Specification
                .where(equal("status", status))
                .and(equal("businessId", businessId))
                .and(equal("vehicleType", vehicleType))
                .and(equal("fuelType", fuelType))
                .and(equal("ownershipType", ownershipType))
                .and(equal("assignedUserId", assignedUserId))
                .and(likeIgnoreCase("department", department))
                .and(likeIgnoreCase("licensePlate", licensePlate));
    }

    public static Specification<Vehicle> assignedTo(Long userId) {
        return equal("assignedUserId", userId);
    }

    private static <T> Specification<Vehicle> equal(String field, T value) {
        return (root, query, cb) -> value == null ? cb.conjunction() : cb.equal(root.get(field), value);
    }

    private static Specification<Vehicle> likeIgnoreCase(String field, String value) {
        return (root, query, cb) -> {
            if (!StringUtils.hasText(value)) {
                return cb.conjunction();
            }
            return cb.like(cb.lower(root.get(field)), "%" + value.trim().toLowerCase() + "%");
        };
    }
}
