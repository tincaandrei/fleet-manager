package com.fleet.parser.controller;

import com.fleet.parser.dto.HealthResponse;
import com.fleet.parser.dto.MeResponse;
import com.fleet.parser.dto.ParserExtractionCommand;
import com.fleet.parser.dto.ParserExtractionResponse;
import com.fleet.parser.service.JwtPrincipal;
import com.fleet.parser.service.ParserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@Tag(name = "Parser", description = "Internal document parser APIs.")
public class ParserController {

    private final String ollamaBaseUrl;
    private final ParserService parserService;

    public ParserController(@Value("${parser.ollama.base-url}") String ollamaBaseUrl,
                            ParserService parserService) {
        this.ollamaBaseUrl = ollamaBaseUrl;
        this.parserService = parserService;
    }

    @GetMapping("/health")
    @Operation(summary = "Health check", description = "Returns parser-service status.")
    public ResponseEntity<HealthResponse> health() {
        return ResponseEntity.ok(new HealthResponse("parser-service", "UP", ollamaBaseUrl));
    }

    @GetMapping("/me")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Current JWT principal", description = "Debug endpoint for JWT wiring.")
    public ResponseEntity<MeResponse> me(Authentication authentication) {
        JwtPrincipal principal = (JwtPrincipal) authentication.getPrincipal();
        return ResponseEntity.ok(new MeResponse(principal.username(), principal.userId(), principal.roles()));
    }

    @PostMapping(
            value = "/documents/extract",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @SecurityRequirement(name = "internalApiKey")
    @Operation(
            summary = "Extract structured data from a PDF",
            description = "Internal endpoint called by document-service. Upload a PDF and receive normalized parser draft data.",
            parameters = {
                    @Parameter(
                            name = "X-Internal-Api-Key",
                            in = ParameterIn.HEADER,
                            required = true,
                            example = "dev-parser-internal-api-key-change-me-32",
                            description = "Internal API key shared with document-service."
                    )
            }
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Extraction completed or failed gracefully inside the stable parser response.",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ParserExtractionResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "documentId": "00000000-0000-0000-0000-000000000001",
                                      "parserStatus": "PARSED",
                                      "detectedDocumentType": "INSURANCE",
                                      "detectedSubtype": "RCA",
                                      "confidence": 0.86,
                                      "parserName": "ollama-document-parser",
                                      "parserVersion": "1.0.0",
                                      "extractionMethod": "PDF_TEXT",
                                      "extractedData": {
                                        "policyNumber": "RO/...",
                                        "licensePlate": "B123XYZ",
                                        "vin": "WVWZZZ1KZAW123456",
                                        "validFrom": "2026-01-01",
                                        "validUntil": "2027-01-01"
                                      },
                                      "warnings": []
                                    }
                                    """)
                    )
            ),
            @ApiResponse(responseCode = "401", description = "Invalid internal API key")
    })
    public ResponseEntity<ParserExtractionResponse> extract(
            @Parameter(description = "PDF document to parse", required = true)
            @RequestPart("file") MultipartFile file,

            @Parameter(description = "Document identifier from document-service", required = true, example = "00000000-0000-0000-0000-000000000001")
            @RequestPart("documentId") String documentId,

            @Parameter(description = "Vehicle identifier from document-service", required = true, example = "42")
            @RequestPart("vehicleId") String vehicleId,

            @Parameter(description = "Optional declared document type, e.g. RCA, ITP, ROVINIETA, EXPENSE_INVOICE")
            @RequestPart(value = "declaredDocumentType", required = false) String declaredDocumentType,

            @Parameter(description = "Original uploaded file name", example = "insurance.pdf")
            @RequestPart(value = "originalFileName", required = false) String originalFileName
    ) {
        ParserExtractionCommand command = new ParserExtractionCommand(
                file,
                documentId,
                vehicleId,
                declaredDocumentType,
                originalFileName
        );
        return ResponseEntity.ok(parserService.extract(command));
    }
}
