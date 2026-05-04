package com.fleet.fleet.repository;

import com.fleet.fleet.entity.Vehicle;
import com.fleet.fleet.entity.VehicleStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface VehicleRepository extends JpaRepository<Vehicle, Long>, JpaSpecificationExecutor<Vehicle> {

    boolean existsByLicensePlateIgnoreCase(String licensePlate);

    boolean existsByVinIgnoreCase(String vin);

    List<Vehicle> findByStatus(VehicleStatus status);
}
