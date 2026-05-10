package com.fleet.document.controller;

import com.fleet.document.dto.DocumentResponse;
import com.fleet.document.dto.ErrorResponse;
import com.fleet.document.dto.ReviewDocumentRequest;
import com.fleet.document.entity.DocumentType;
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
import org.springframework.web.bind.annotation.RequestMapping;
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

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload document", description = "Uploads a PDF for a vehicle, stores it, runs the local Python parser, and marks it as NEEDS_REVIEW when parsing succeeds.")
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
            @RequestParam Long vehicleId,
            @Parameter(description = "Document type.", example = "INSPECTION", required = true)
            @RequestParam DocumentType documentType,
            HttpServletRequest servletRequest,
            Authentication authentication
    ) {
        return ResponseEntity.ok(documentService.upload(
                file,
                vehicleId,
                documentType,
                servletRequest.getHeader(HttpHeaders.AUTHORIZATION),
                authentication
        ));
    }

    @GetMapping
    @Operation(summary = "List documents by vehicle", description = "USER sees only validated documents with approved data. STAFF/ADMIN see all statuses and raw extraction data.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Documents returned", content = @Content(array = @ArraySchema(schema = @Schema(implementation = DocumentResponse.class)))),
            @ApiResponse(responseCode = "400", description = "Vehicle id is missing or invalid"),
            @ApiResponse(responseCode = "401", description = "Authentication required")
    })
    public ResponseEntity<List<DocumentResponse>> listByVehicle(
            @Parameter(description = "Vehicle id.", example = "1", required = true)
            @RequestParam Long vehicleId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(documentService.listByVehicle(vehicleId, authentication));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get document details", description = "Returns document metadata. STAFF/ADMIN also receive raw extraction data.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Document returned", content = @Content(schema = @Schema(implementation = DocumentResponse.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "404", description = "Document not found")
    })
    public ResponseEntity<DocumentResponse> getById(
            @Parameter(description = "Document id.") @PathVariable UUID id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(documentService.getById(id, authentication));
    }

    @GetMapping("/{id}/download")
    @Operation(summary = "Download document", description = "Downloads the stored PDF file. Requires authentication.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "PDF file returned"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "404", description = "Document not found")
    })
    public ResponseEntity<Resource> download(
            @Parameter(description = "Document id.") @PathVariable UUID id
    ) {
        VehicleDocument document = documentService.getDownloadMetadata(id);
        Resource resource = documentService.download(id);
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
    public ResponseEntity<List<DocumentResponse>> reviewQueue() {
        return ResponseEntity.ok(documentService.reviewQueue());
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

    @PatchMapping("/{id}/archive")
    @Operation(summary = "Archive document", description = "Marks a document as ARCHIVED. Requires ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Document archived", content = @Content(schema = @Schema(implementation = DocumentResponse.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "ADMIN role required"),
            @ApiResponse(responseCode = "404", description = "Document not found")
    })
    public ResponseEntity<DocumentResponse> archive(
            @Parameter(description = "Document id.") @PathVariable UUID id
    ) {
        return ResponseEntity.ok(documentService.archive(id));
    }
}
