package com.fleet.fleet.controller;

import com.fleet.fleet.dto.VehicleAssignmentRequest;
import com.fleet.fleet.dto.VehicleRequest;
import com.fleet.fleet.dto.VehicleResponse;
import com.fleet.fleet.dto.VehicleStatusRequest;
import com.fleet.fleet.entity.FuelType;
import com.fleet.fleet.entity.OwnershipType;
import com.fleet.fleet.entity.VehicleStatus;
import com.fleet.fleet.entity.VehicleType;
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
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Vehicles", description = "Vehicle registry endpoints.")
public class VehicleController {

    private static final String VEHICLE_REQUEST_EXAMPLE = """
            {
              "licensePlate": "B-123-ABC",
              "vin": "1HGCM82633A004352",
              "brand": "Toyota",
              "model": "Corolla",
              "manufactureYear": 2022,
              "vehicleType": "CAR",
              "fuelType": "HYBRID",
              "ownershipType": "OWNED",
              "status": "ACTIVE",
              "department": "Operations",
              "currentMileage": 25000
            }
            """;

    private static final String VEHICLE_RESPONSE_EXAMPLE = """
            {
              "id": 1,
              "licensePlate": "B-123-ABC",
              "vin": "1HGCM82633A004352",
              "brand": "Toyota",
              "model": "Corolla",
              "manufactureYear": 2022,
              "vehicleType": "CAR",
              "fuelType": "HYBRID",
              "ownershipType": "OWNED",
              "status": "ACTIVE",
              "department": "Operations",
              "assignedUserId": 12,
              "assignedDriverName": "Alex Ionescu",
              "currentMileage": 25000,
              "createdAt": "2026-05-04T11:30:00Z",
              "updatedAt": "2026-05-04T11:45:00Z"
            }
            """;

    private static final String VEHICLE_LIST_RESPONSE_EXAMPLE = """
            [
              {
                "id": 1,
                "licensePlate": "B-123-ABC",
                "vin": "1HGCM82633A004352",
                "brand": "Toyota",
                "model": "Corolla",
                "manufactureYear": 2022,
                "vehicleType": "CAR",
                "fuelType": "HYBRID",
                "ownershipType": "OWNED",
                "status": "ACTIVE",
                "department": "Operations",
                "assignedUserId": 12,
                "assignedDriverName": "Alex Ionescu",
                "currentMileage": 25000,
                "createdAt": "2026-05-04T11:30:00Z",
                "updatedAt": "2026-05-04T11:45:00Z"
              }
            ]
            """;

    private static final String STATUS_REQUEST_EXAMPLE = """
            {
              "status": "IN_SERVICE"
            }
            """;

    private static final String ASSIGNMENT_REQUEST_EXAMPLE = """
            {
              "assignedUserId": 12
            }
            """;

    private final VehicleService vehicleService;

    @PostMapping("/vehicles")
    @Operation(
            summary = "Create vehicle",
            description = "Creates a vehicle registry record. `licensePlate` is required and must be unique. `status` defaults to `ACTIVE` when omitted. Requires `ADMIN` or `FLEET_MANAGER`."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Vehicle created", content = @Content(schema = @Schema(implementation = VehicleResponse.class), examples = @ExampleObject(value = VEHICLE_RESPONSE_EXAMPLE))),
            @ApiResponse(responseCode = "400", description = "Validation failed or invalid enum value"),
            @ApiResponse(responseCode = "401", description = "JWT token is missing or invalid"),
            @ApiResponse(responseCode = "403", description = "Role is not allowed to create vehicles"),
            @ApiResponse(responseCode = "409", description = "License plate or VIN already exists")
    })
    public ResponseEntity<VehicleResponse> create(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Vehicle data to create.",
                    required = true,
                    content = @Content(schema = @Schema(implementation = VehicleRequest.class), examples = @ExampleObject(value = VEHICLE_REQUEST_EXAMPLE))
            )
            @Valid @RequestBody VehicleRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(vehicleService.create(request, authentication));
    }

    @GetMapping("/vehicles")
    @Operation(
            summary = "List vehicles",
            description = "Returns vehicles visible to the authenticated user. Admin, fleet manager, and auditor roles can read all vehicles. Employee/User roles only see assigned vehicles when the JWT contains a numeric user id."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Vehicles returned", content = @Content(array = @ArraySchema(schema = @Schema(implementation = VehicleResponse.class)), examples = @ExampleObject(value = VEHICLE_LIST_RESPONSE_EXAMPLE))),
            @ApiResponse(responseCode = "400", description = "Invalid filter value"),
            @ApiResponse(responseCode = "401", description = "JWT token is missing or invalid"),
            @ApiResponse(responseCode = "403", description = "Role is not allowed to list vehicles")
    })
    public ResponseEntity<List<VehicleResponse>> list(
            @Parameter(description = "Filter by operational status.", example = "ACTIVE")
            @RequestParam(required = false) VehicleStatus status,
            @Parameter(description = "Filter by vehicle category.", example = "CAR")
            @RequestParam(required = false) VehicleType vehicleType,
            @Parameter(description = "Filter by fuel or powertrain type.", example = "HYBRID")
            @RequestParam(required = false) FuelType fuelType,
            @Parameter(description = "Filter by ownership model.", example = "OWNED")
            @RequestParam(required = false) OwnershipType ownershipType,
            @Parameter(description = "Filter by responsible department.", example = "Operations")
            @RequestParam(required = false) String department,
            @Parameter(description = "Filter by assigned application user id.", example = "12")
            @RequestParam(required = false) Long assignedUserId,
            @Parameter(description = "Filter by exact license plate.", example = "B-123-ABC")
            @RequestParam(required = false) String licensePlate,
            @Parameter(description = "Filter by business id. SUPERADMIN only.", example = "1")
            @RequestParam(required = false) Long businessId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(vehicleService.findAll(
                status,
                vehicleType,
                fuelType,
                ownershipType,
                department,
                assignedUserId,
                licensePlate,
                businessId,
                authentication
        ));
    }

    @GetMapping("/vehicles/{id}")
    @Operation(
            summary = "Get vehicle by id",
            description = "Returns one vehicle by id. Employee/User roles can access only vehicles assigned to their user id."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Vehicle returned", content = @Content(schema = @Schema(implementation = VehicleResponse.class), examples = @ExampleObject(value = VEHICLE_RESPONSE_EXAMPLE))),
            @ApiResponse(responseCode = "401", description = "JWT token is missing or invalid"),
            @ApiResponse(responseCode = "403", description = "Role or assignment does not allow reading this vehicle"),
            @ApiResponse(responseCode = "404", description = "Vehicle was not found")
    })
    public ResponseEntity<VehicleResponse> getById(
            @Parameter(description = "Vehicle id.", example = "1") @PathVariable Long id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(vehicleService.getById(id, authentication));
    }

    @PutMapping("/vehicles/{id}")
    @Operation(
            summary = "Update vehicle",
            description = "Replaces editable vehicle fields with the supplied payload. Requires `ADMIN` or `FLEET_MANAGER`."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Vehicle updated", content = @Content(schema = @Schema(implementation = VehicleResponse.class), examples = @ExampleObject(value = VEHICLE_RESPONSE_EXAMPLE))),
            @ApiResponse(responseCode = "400", description = "Validation failed or invalid enum value"),
            @ApiResponse(responseCode = "401", description = "JWT token is missing or invalid"),
            @ApiResponse(responseCode = "403", description = "Role is not allowed to update vehicles"),
            @ApiResponse(responseCode = "404", description = "Vehicle was not found"),
            @ApiResponse(responseCode = "409", description = "License plate or VIN already exists")
    })
    public ResponseEntity<VehicleResponse> update(
            @Parameter(description = "Vehicle id.", example = "1") @PathVariable Long id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Replacement vehicle data.",
                    required = true,
                    content = @Content(schema = @Schema(implementation = VehicleRequest.class), examples = @ExampleObject(value = VEHICLE_REQUEST_EXAMPLE))
            )
            @Valid @RequestBody VehicleRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(vehicleService.update(id, request, authentication));
    }

    @PatchMapping("/vehicles/{id}/status")
    @Operation(
            summary = "Change vehicle status",
            description = "Updates only the operational status of a vehicle. Requires `ADMIN` or `FLEET_MANAGER`."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Vehicle status changed", content = @Content(schema = @Schema(implementation = VehicleResponse.class), examples = @ExampleObject(value = VEHICLE_RESPONSE_EXAMPLE))),
            @ApiResponse(responseCode = "400", description = "Validation failed or invalid status"),
            @ApiResponse(responseCode = "401", description = "JWT token is missing or invalid"),
            @ApiResponse(responseCode = "403", description = "Role is not allowed to change vehicle status"),
            @ApiResponse(responseCode = "404", description = "Vehicle was not found")
    })
    public ResponseEntity<VehicleResponse> changeStatus(
            @Parameter(description = "Vehicle id.", example = "1") @PathVariable Long id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "New vehicle status.",
                    required = true,
                    content = @Content(schema = @Schema(implementation = VehicleStatusRequest.class), examples = @ExampleObject(value = STATUS_REQUEST_EXAMPLE))
            )
            @Valid @RequestBody VehicleStatusRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(vehicleService.changeStatus(id, request.status(), authentication));
    }

    @PatchMapping("/vehicles/{id}/assignment")
    @Operation(
            summary = "Assign vehicle",
            description = "Assigns an active employee from the vehicle organization, or clears the current driver. Requires same-organization `BUSINESS_ADMIN`."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Vehicle assignment changed", content = @Content(schema = @Schema(implementation = VehicleResponse.class), examples = @ExampleObject(value = VEHICLE_RESPONSE_EXAMPLE))),
            @ApiResponse(responseCode = "400", description = "Validation failed"),
            @ApiResponse(responseCode = "401", description = "JWT token is missing or invalid"),
            @ApiResponse(responseCode = "403", description = "Role is not allowed to assign vehicles"),
            @ApiResponse(responseCode = "404", description = "Vehicle was not found")
    })
    public ResponseEntity<VehicleResponse> assign(
            @Parameter(description = "Vehicle id.", example = "1") @PathVariable Long id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Assignment details. Nullable fields can be used to clear existing assignment data.",
                    required = true,
                    content = @Content(schema = @Schema(implementation = VehicleAssignmentRequest.class), examples = @ExampleObject(value = ASSIGNMENT_REQUEST_EXAMPLE))
            )
            @Valid @RequestBody VehicleAssignmentRequest request,
            Authentication authentication,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader
    ) {
        return ResponseEntity.ok(vehicleService.assign(id, request, authentication, authorizationHeader));
    }

    @PostMapping(path = "/vehicles/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Upload vehicle image",
            description = "Uploads or replaces the display image for a vehicle. Accepts JPG, PNG, or WebP up to 5 MB. Requires `ADMIN` or `FLEET_MANAGER`."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Vehicle image saved", content = @Content(schema = @Schema(implementation = VehicleResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid image"),
            @ApiResponse(responseCode = "401", description = "JWT token is missing or invalid"),
            @ApiResponse(responseCode = "403", description = "Role is not allowed to update vehicles"),
            @ApiResponse(responseCode = "404", description = "Vehicle was not found")
    })
    public ResponseEntity<VehicleResponse> uploadImage(
            @Parameter(description = "Vehicle id.", example = "1") @PathVariable Long id,
            @Parameter(description = "Image file to upload.", required = true)
            @RequestPart("file") MultipartFile file,
            Authentication authentication
    ) {
        return ResponseEntity.ok(vehicleService.uploadImage(id, file, authentication));
    }

    @GetMapping("/vehicles/{id}/image")
    @Operation(
            summary = "Download vehicle image",
            description = "Streams the vehicle display image. Employee/User roles can access only images for assigned vehicles."
    )
    public ResponseEntity<Resource> image(
            @Parameter(description = "Vehicle id.", example = "1") @PathVariable Long id,
            Authentication authentication
    ) {
        VehicleService.VehicleImageResource image = vehicleService.loadImage(id, authentication);
        MediaType mediaType = image.contentType() == null
                ? MediaType.APPLICATION_OCTET_STREAM
                : MediaType.parseMediaType(image.contentType());
        ContentDisposition disposition = ContentDisposition.inline()
                .filename(image.originalFileName() == null ? "vehicle-image" : image.originalFileName())
                .build();
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(image.resource());
    }

    @DeleteMapping("/vehicles/{id}/image")
    @Operation(
            summary = "Delete vehicle image",
            description = "Removes the display image from a vehicle. Requires `ADMIN` or `FLEET_MANAGER`."
    )
    public ResponseEntity<VehicleResponse> deleteImage(
            @Parameter(description = "Vehicle id.", example = "1") @PathVariable Long id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(vehicleService.deleteImage(id, authentication));
    }

    @DeleteMapping("/vehicles/{id}")
    @Operation(
            summary = "Delete vehicle",
            description = "Deletes a vehicle registry record. Requires `ADMIN`."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Vehicle deleted"),
            @ApiResponse(responseCode = "401", description = "JWT token is missing or invalid"),
            @ApiResponse(responseCode = "403", description = "Role is not allowed to delete vehicles"),
            @ApiResponse(responseCode = "404", description = "Vehicle was not found")
    })
    public ResponseEntity<Void> delete(
            @Parameter(description = "Vehicle id.", example = "1") @PathVariable Long id,
            Authentication authentication
    ) {
        vehicleService.delete(id, authentication);
        return ResponseEntity.noContent().build();
    }
}
