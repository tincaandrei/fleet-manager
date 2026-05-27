package com.fleet.fleet.repository;

import com.fleet.fleet.entity.Vehicle;
import com.fleet.fleet.entity.VehicleStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface VehicleRepository extends JpaRepository<Vehicle, Long>, JpaSpecificationExecutor<Vehicle> {

    boolean existsByBusinessIdAndLicensePlateIgnoreCase(Long businessId, String licensePlate);

    boolean existsByBusinessIdAndVinIgnoreCase(Long businessId, String vin);

    List<Vehicle> findByStatus(VehicleStatus status);

    List<Vehicle> findByBusinessIdAndStatus(Long businessId, VehicleStatus status);
}
