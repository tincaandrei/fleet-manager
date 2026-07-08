package com.fleet.document;

import com.fleet.document.dto.VehicleBasicInfoResponse;
import com.fleet.document.entity.DocumentStatus;
import com.fleet.document.entity.VehicleDocument;
import com.fleet.document.repository.VehicleDocumentRepository;
import com.fleet.document.service.AuthUserLookupClient;
import com.fleet.document.service.DocumentParsingOutboxService;
import com.fleet.document.service.FleetVehicleClient;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "security.jwt.secret=01234567890123456789012345678901",
        "spring.datasource.url=jdbc:h2:mem:document-integration;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false",
        "document.storage.path=${java.io.tmpdir}/document-api-integration-storage",
        "openai.api-key=test-key",
        "app.rabbitmq.enabled=false"
})
class DocumentApiIntegrationTest {

    private static final String BEARER = "Bearer ";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private VehicleDocumentRepository documentRepository;

    @MockBean
    private FleetVehicleClient fleetVehicleClient;

    @MockBean
    private AuthUserLookupClient authUserLookupClient;

    @MockBean
    private DocumentParsingOutboxService parsingOutboxService;

    @BeforeEach
    void setUp() {
        documentRepository.deleteAll();
    }

    @Test
    void authenticatedAdminCanUploadAndListDocument() throws Exception {
        String token = token("atlas.admin", 1001L, 101L, "BUSINESS_ADMIN");
        when(fleetVehicleClient.vehicleBasicInfo(eq(1001L), eq(BEARER + token)))
                .thenReturn(new VehicleBasicInfoResponse(
                        1001L, 101L, "B-101-ATL", "Dacia", "Duster", "ACTIVE", null, null
                ));
        when(authUserLookupClient.lookupBusinessAdmins(eq(101L), eq(BEARER + token)))
                .thenReturn(List.of());

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "insurance.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                "%PDF-1.4 integration test".getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/").file(file)
                        .param("vehicleId", "1001")
                        .header(HttpHeaders.AUTHORIZATION, BEARER + token))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.vehicleId").value(1001))
                .andExpect(jsonPath("$.originalFileName").value("insurance.pdf"))
                .andExpect(jsonPath("$.status").value("PARSING"));

        assertThat(documentRepository.count()).isEqualTo(1);

        when(fleetVehicleClient.vehicleBasicInfo(eq(1001L), eq(BEARER + token)))
                .thenReturn(new VehicleBasicInfoResponse(
                        1001L, 101L, "B-101-ATL", "Dacia", "Duster", "ACTIVE", null, null
                ));
        mockMvc.perform(get("/").param("vehicleId", "1001")
                        .header(HttpHeaders.AUTHORIZATION, BEARER + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].originalFileName").value("insurance.pdf"));
    }

    @Test
    void unauthenticatedUploadIsRejected() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "insurance.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                "%PDF-1.4".getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/").file(file).param("vehicleId", "1001"))
                .andExpect(status().isUnauthorized());

        assertThat(documentRepository.count()).isZero();
    }

    @Test
    void superadminHistorySupportsOptionalBusinessFilterAndKeepsGlobalDefault() throws Exception {
        storedDocument(1001L, 101L, "acme-invoice.pdf");
        storedDocument(2002L, 202L, "globex-invoice.pdf");
        String superadminToken = token("root", 1L, null, "SUPERADMIN");

        mockMvc.perform(get("/history")
                        .header(HttpHeaders.AUTHORIZATION, BEARER + superadminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));

        mockMvc.perform(get("/history")
                        .param("businessId", "101")
                        .header(HttpHeaders.AUTHORIZATION, BEARER + superadminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.items[0].businessId").value(101))
                .andExpect(jsonPath("$.items[0].originalFileName").value("acme-invoice.pdf"));

        mockMvc.perform(get("/history/export")
                        .param("businessId", "101")
                        .header(HttpHeaders.AUTHORIZATION, BEARER + superadminToken))
                .andExpect(status().isOk());
    }

    @Test
    void businessAdminHistoryIgnoresForeignBusinessIdFilter() throws Exception {
        storedDocument(1001L, 101L, "acme-invoice.pdf");
        storedDocument(2002L, 202L, "globex-invoice.pdf");
        String adminToken = token("atlas.admin", 11L, 101L, "BUSINESS_ADMIN");

        mockMvc.perform(get("/history")
                        .param("businessId", "202")
                        .header(HttpHeaders.AUTHORIZATION, BEARER + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.items[0].businessId").value(101));
    }

    @Test
    void vehicleCostExportScopesBusinessFilterToSuperadminOnly() throws Exception {
        when(fleetVehicleClient.visibleVehicles(any(), any())).thenReturn(List.of());

        String superadminToken = token("root", 1L, null, "SUPERADMIN");
        mockMvc.perform(get("/reports/vehicle-costs/export")
                        .param("businessId", "101")
                        .header(HttpHeaders.AUTHORIZATION, BEARER + superadminToken))
                .andExpect(status().isOk());
        verify(fleetVehicleClient).visibleVehicles(eq(BEARER + superadminToken), eq(101L));

        String adminToken = token("atlas.admin", 11L, 101L, "BUSINESS_ADMIN");
        mockMvc.perform(get("/reports/vehicle-costs/export")
                        .param("businessId", "202")
                        .header(HttpHeaders.AUTHORIZATION, BEARER + adminToken))
                .andExpect(status().isOk());
        verify(fleetVehicleClient).visibleVehicles(eq(BEARER + adminToken), isNull());
    }

    private void storedDocument(Long vehicleId, Long businessId, String fileName) {
        documentRepository.save(VehicleDocument.builder()
                .vehicleId(vehicleId)
                .businessId(businessId)
                .status(DocumentStatus.VALIDATED)
                .originalFileName(fileName)
                .storedFileName(fileName)
                .contentType(MediaType.APPLICATION_PDF_VALUE)
                .fileSize(128L)
                .storagePath("integration/" + fileName)
                .uploadedByUserId(11L)
                .build());
    }

    private String token(String subject, Long userId, Long businessId, String role) {
        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("role", role);
        claims.put("roles", List.of(role));
        claims.put("userId", userId);
        claims.put("businessId", businessId);
        return Jwts.builder()
                .addClaims(claims)
                .setSubject(subject)
                .setIssuedAt(Date.from(Instant.now()))
                .setExpiration(Date.from(Instant.now().plusSeconds(3600)))
                .signWith(
                        Keys.hmacShaKeyFor("01234567890123456789012345678901".getBytes(StandardCharsets.UTF_8)),
                        SignatureAlgorithm.HS256
                )
                .compact();
    }
}
