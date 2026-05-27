package com.fleet.document.controller;

import com.fleet.document.dto.ApproveDocumentRequest;
import com.fleet.document.dto.ApprovedDocumentDataResponse;
import com.fleet.document.dto.DocumentResponse;
import com.fleet.document.dto.DocumentInfoFolderResponse;
import com.fleet.document.dto.ErrorResponse;
import com.fleet.document.dto.ParserResultRequest;
import com.fleet.document.dto.RejectDocumentRequest;
import com.fleet.document.dto.ReviewDocumentRequest;
import com.fleet.document.dto.VehicleAlertGroupResponse;
import com.fleet.document.dto.VehicleDocumentAttributeResponse;
import com.fleet.document.entity.VehicleDocument;
import com.fleet.document.service.DocumentService;
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
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Documents", description = "Vehicle document upload, review, and download endpoints.")
public class DocumentController {

    private static final String REVIEW_REQUEST_EXAMPLE = """
            {
              "decision": "APPROVE",
              "approvedData": {
                "documentType": "INSPECTION",
                "inspectionNumber": "MOCK-ITP-2026-001",
                "expiryDate": "2027-03-10"
              },
              "comment": "Approved after review"
            }
            """;

    private final DocumentService documentService;

    @PostMapping(path = {"", "/"}, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload document", description = "Uploads a PDF for a vehicle, stores it, and marks it as PARSING.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Document uploaded", content = @Content(schema = @Schema(implementation = DocumentResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation failed", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Role is not allowed to upload")
    })
    public ResponseEntity<DocumentResponse> upload(
            @Parameter(description = "PDF file to upload.", required = true)
            @RequestPart("file") MultipartFile file,
            @Parameter(description = "Vehicle id.", example = "1", required = true)
            @RequestParam(name = "vehicleId") Long vehicleId,
            HttpServletRequest servletRequest,
            Authentication authentication
    ) {
        return ResponseEntity.ok(documentService.upload(
                file,
                vehicleId,
                servletRequest.getHeader(HttpHeaders.AUTHORIZATION),
                authentication
        ));
    }

    @PostMapping("/{id}/parser-result")
    @Operation(summary = "Receive parser result", description = "Stores parser output as an unapproved extraction draft.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Parser result accepted", content = @Content(schema = @Schema(implementation = DocumentResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid parser result"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "404", description = "Document not found")
    })
    public ResponseEntity<DocumentResponse> receiveParserResult(
            @Parameter(description = "Document id.") @PathVariable UUID id,
            @Valid @RequestBody ParserResultRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(documentService.receiveParserResult(id, request, authentication));
    }

    @GetMapping
    @Operation(summary = "List documents by vehicle", description = "USER sees only validated documents with approved data. STAFF/ADMIN see all document statuses.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Documents returned", content = @Content(array = @ArraySchema(schema = @Schema(implementation = DocumentResponse.class)))),
            @ApiResponse(responseCode = "400", description = "Vehicle id is missing or invalid"),
            @ApiResponse(responseCode = "401", description = "Authentication required")
    })
    public ResponseEntity<List<DocumentResponse>> listByVehicle(
            @Parameter(description = "Vehicle id.", example = "1", required = true)
            @RequestParam(name = "vehicleId") Long vehicleId,
            HttpServletRequest servletRequest,
            Authentication authentication
    ) {
        return ResponseEntity.ok(documentService.listByVehicle(
                vehicleId,
                servletRequest.getHeader(HttpHeaders.AUTHORIZATION),
                authentication
        ));
    }

    @GetMapping("/vehicles/{vehicleId}/documents")
    @Operation(summary = "List documents by vehicle", description = "Path-based alias for listing vehicle documents.")
    public ResponseEntity<List<DocumentResponse>> listVehicleDocuments(
            @Parameter(description = "Vehicle id.", example = "1", required = true)
            @PathVariable Long vehicleId,
            HttpServletRequest servletRequest,
            Authentication authentication
    ) {
        return ResponseEntity.ok(documentService.getVehicleDocuments(
                vehicleId,
                servletRequest.getHeader(HttpHeaders.AUTHORIZATION),
                authentication
        ));
    }

    @GetMapping("/vehicles/{vehicleId}/approved-document-data")
    @Operation(summary = "List approved document data by vehicle", description = "Returns official approved document data for a vehicle.")
    public ResponseEntity<List<ApprovedDocumentDataResponse>> listApprovedVehicleDocumentData(
            @Parameter(description = "Vehicle id.", example = "1", required = true)
            @PathVariable Long vehicleId,
            HttpServletRequest servletRequest,
            Authentication authentication
    ) {
        return ResponseEntity.ok(documentService.getApprovedVehicleDocumentData(
                vehicleId,
                servletRequest.getHeader(HttpHeaders.AUTHORIZATION),
                authentication
        ));
    }

    @GetMapping("/vehicles/{vehicleId}/attributes")
    @Operation(
            summary = "List active vehicle document attributes",
            description = "Returns normalized approved document attributes for a vehicle. These records are used for expiration alerts."
    )
    public ResponseEntity<List<VehicleDocumentAttributeResponse>> listVehicleDocumentAttributes(
            @Parameter(description = "Vehicle id.", example = "1", required = true)
            @PathVariable Long vehicleId,
            HttpServletRequest servletRequest
    ) {
        return ResponseEntity.ok(documentService.getVehicleDocumentAttributes(
                vehicleId,
                servletRequest.getHeader(HttpHeaders.AUTHORIZATION)
        ));
    }

    @GetMapping("/alerts/document-expirations")
    @Operation(
            summary = "List document expiration alert candidates",
            description = "Returns active vehicle document attributes that are expired or expire within the requested number of days. Requires STAFF or ADMIN."
    )
    public ResponseEntity<List<VehicleDocumentAttributeResponse>> listDocumentExpirationAlerts(
            @Parameter(description = "Days ahead to include.", example = "30")
            @RequestParam(name = "days", required = false, defaultValue = "30") Integer days,
            @Parameter(description = "Include already expired documents.", example = "true")
            @RequestParam(name = "includeExpired", required = false, defaultValue = "true") boolean includeExpired,
            HttpServletRequest servletRequest,
            Authentication authentication
    ) {
        return ResponseEntity.ok(documentService.getExpiringVehicleDocumentAttributes(
                days,
                includeExpired,
                servletRequest.getHeader(HttpHeaders.AUTHORIZATION),
                authentication
        ));
    }

    @GetMapping("/alerts/vehicles")
    @Operation(summary = "Grouped vehicle document alerts", description = "Returns visible vehicles with their expired/upcoming document alerts.")
    public ResponseEntity<List<VehicleAlertGroupResponse>> groupedVehicleAlerts(
            @RequestParam(name = "days", required = false, defaultValue = "30") Integer days,
            @RequestParam(name = "includeExpired", required = false, defaultValue = "true") boolean includeExpired,
            HttpServletRequest servletRequest
    ) {
        return ResponseEntity.ok(documentService.getGroupedVehicleAlerts(
                days,
                includeExpired,
                servletRequest.getHeader(HttpHeaders.AUTHORIZATION)
        ));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get document details", description = "Returns document metadata and approved review data when present.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Document returned", content = @Content(schema = @Schema(implementation = DocumentResponse.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "404", description = "Document not found")
    })
    public ResponseEntity<DocumentResponse> getById(
            @Parameter(description = "Document id.") @PathVariable UUID id,
            HttpServletRequest servletRequest,
            Authentication authentication
    ) {
        return ResponseEntity.ok(documentService.getById(
                id,
                servletRequest.getHeader(HttpHeaders.AUTHORIZATION),
                authentication
        ));
    }

    @GetMapping("/{id}/info-folder")
    @Operation(summary = "Document info folder", description = "Returns canonical and extra extracted metadata for a document.")
    public ResponseEntity<DocumentInfoFolderResponse> infoFolder(
            @Parameter(description = "Document id.") @PathVariable UUID id,
            HttpServletRequest servletRequest,
            Authentication authentication
    ) {
        return ResponseEntity.ok(documentService.getInfoFolder(
                id,
                servletRequest.getHeader(HttpHeaders.AUTHORIZATION),
                authentication
        ));
    }

    @GetMapping("/{id}/download")
    @Operation(summary = "Download document", description = "Downloads the stored PDF file. Requires authentication.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "PDF file returned"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "404", description = "Document not found")
    })
    public ResponseEntity<Resource> download(
            @Parameter(description = "Document id.") @PathVariable UUID id,
            HttpServletRequest servletRequest,
            Authentication authentication
    ) {
        String authorizationHeader = servletRequest.getHeader(HttpHeaders.AUTHORIZATION);
        VehicleDocument document = documentService.getDownloadMetadata(id, authorizationHeader, authentication);
        Resource resource = documentService.download(id, authorizationHeader, authentication);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(document.getOriginalFileName())
                        .build()
                        .toString())
                .body(resource);
    }

    @GetMapping("/review-queue")
    @Operation(summary = "Review queue", description = "Returns documents with NEEDS_REVIEW status. Requires STAFF or ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Review queue returned", content = @Content(array = @ArraySchema(schema = @Schema(implementation = DocumentResponse.class)))),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "STAFF or ADMIN role required")
    })
    public ResponseEntity<List<DocumentResponse>> reviewQueue(Authentication authentication) {
        return ResponseEntity.ok(documentService.reviewQueue(authentication));
    }

    @PostMapping("/{id}/review")
    @Operation(summary = "Review document", description = "Approves or rejects extracted document data. Requires STAFF or ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Review saved", content = @Content(schema = @Schema(implementation = DocumentResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid review decision or approved data missing"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "STAFF or ADMIN role required"),
            @ApiResponse(responseCode = "404", description = "Document not found")
    })
    public ResponseEntity<DocumentResponse> review(
            @Parameter(description = "Document id.") @PathVariable UUID id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Review decision.",
                    required = true,
                    content = @Content(schema = @Schema(implementation = ReviewDocumentRequest.class), examples = @ExampleObject(value = REVIEW_REQUEST_EXAMPLE))
            )
            @Valid @RequestBody ReviewDocumentRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(documentService.review(id, request, authentication));
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "Approve document extraction", description = "Creates or updates official approved document data. Requires STAFF or ADMIN.")
    public ResponseEntity<DocumentResponse> approve(
            @Parameter(description = "Document id.") @PathVariable UUID id,
            @Valid @RequestBody ApproveDocumentRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(documentService.approveDocument(id, request, authentication));
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "Reject document extraction", description = "Rejects an extraction draft or failed parsing result. Requires STAFF or ADMIN.")
    public ResponseEntity<DocumentResponse> reject(
            @Parameter(description = "Document id.") @PathVariable UUID id,
            @RequestBody RejectDocumentRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(documentService.rejectDocument(id, request, authentication));
    }

    @PostMapping("/{id}/archive")
    @Operation(summary = "Archive document", description = "Marks a document as ARCHIVED. Requires STAFF or ADMIN.")
    public ResponseEntity<DocumentResponse> archivePost(
            @Parameter(description = "Document id.") @PathVariable UUID id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(documentService.archiveDocument(id, authentication));
    }

    @PatchMapping("/{id}/archive")
    @Operation(summary = "Archive document", description = "Marks a document as ARCHIVED. Requires STAFF or ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Document archived", content = @Content(schema = @Schema(implementation = DocumentResponse.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "ADMIN role required"),
            @ApiResponse(responseCode = "404", description = "Document not found")
    })
    public ResponseEntity<DocumentResponse> archive(
            @Parameter(description = "Document id.") @PathVariable UUID id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(documentService.archiveDocument(id, authentication));
    }
}
