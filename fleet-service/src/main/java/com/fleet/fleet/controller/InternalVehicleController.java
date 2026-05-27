package com.fleet.fleet.controller;

import com.fleet.fleet.dto.VehicleBasicInfoResponse;
import com.fleet.fleet.dto.VehicleExistsResponse;
import com.fleet.fleet.service.VehicleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.Authentication;

import java.util.List;

@RestController
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Internal Vehicles", description = "Internal vehicle lookup endpoints.")
public class InternalVehicleController {

    private static final String EXISTS_RESPONSE_EXAMPLE = """
            {
              "id": 1,
              "exists": true
            }
            """;

    private static final String BASIC_INFO_RESPONSE_EXAMPLE = """
            {
              "id": 1,
              "licensePlate": "B-123-ABC",
              "brand": "Toyota",
              "model": "Corolla",
              "status": "ACTIVE",
              "assignedUserId": 12,
              "assignedDriverName": "Alex Ionescu"
            }
            """;

    private static final String ACTIVE_VEHICLES_RESPONSE_EXAMPLE = """
            [
              {
                "id": 1,
                "licensePlate": "B-123-ABC",
                "brand": "Toyota",
                "model": "Corolla",
                "status": "ACTIVE",
                "assignedUserId": 12,
                "assignedDriverName": "Alex Ionescu"
              }
            ]
            """;

    private final VehicleService vehicleService;

    @GetMapping("/internal/vehicles/{id}/exists")
    @Operation(
            summary = "Check vehicle existence",
            description = "Internal lookup used by other services to check whether a vehicle id exists. Requires `ADMIN`, `FLEET_MANAGER`, or `AUDITOR`."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Existence result returned", content = @Content(schema = @Schema(implementation = VehicleExistsResponse.class), examples = @ExampleObject(value = EXISTS_RESPONSE_EXAMPLE))),
            @ApiResponse(responseCode = "401", description = "JWT token is missing or invalid"),
            @ApiResponse(responseCode = "403", description = "Role is not allowed to use internal vehicle lookups")
    })
    public ResponseEntity<VehicleExistsResponse> exists(
            @Parameter(description = "Vehicle id to check.", example = "1") @PathVariable Long id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(vehicleService.exists(id, authentication));
    }

    @GetMapping("/internal/vehicles/{id}/basic-info")
    @Operation(
            summary = "Get vehicle basic info",
            description = "Internal lookup that returns a compact vehicle representation for cross-service validation or display. Requires `ADMIN`, `FLEET_MANAGER`, or `AUDITOR`."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Basic vehicle info returned", content = @Content(schema = @Schema(implementation = VehicleBasicInfoResponse.class), examples = @ExampleObject(value = BASIC_INFO_RESPONSE_EXAMPLE))),
            @ApiResponse(responseCode = "401", description = "JWT token is missing or invalid"),
            @ApiResponse(responseCode = "403", description = "Role is not allowed to use internal vehicle lookups"),
            @ApiResponse(responseCode = "404", description = "Vehicle was not found")
    })
    public ResponseEntity<VehicleBasicInfoResponse> basicInfo(
            @Parameter(description = "Vehicle id.", example = "1") @PathVariable Long id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(vehicleService.basicInfo(id, authentication));
    }

    @GetMapping("/internal/vehicles/active")
    @Operation(
            summary = "List active vehicles",
            description = "Internal lookup that returns compact records for vehicles with `ACTIVE` status. Requires `ADMIN`, `FLEET_MANAGER`, or `AUDITOR`."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Active vehicles returned", content = @Content(array = @ArraySchema(schema = @Schema(implementation = VehicleBasicInfoResponse.class)), examples = @ExampleObject(value = ACTIVE_VEHICLES_RESPONSE_EXAMPLE))),
            @ApiResponse(responseCode = "401", description = "JWT token is missing or invalid"),
            @ApiResponse(responseCode = "403", description = "Role is not allowed to use internal vehicle lookups")
    })
    public ResponseEntity<List<VehicleBasicInfoResponse>> activeVehicles(
            @RequestParam(required = false) Long businessId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(vehicleService.activeVehicles(businessId, authentication));
    }
}
