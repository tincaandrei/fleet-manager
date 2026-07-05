package com.fleet.document.service;

import com.fleet.document.dto.VehicleBasicInfoResponse;
import com.fleet.document.entity.ApprovedDataStatus;
import com.fleet.document.entity.VehicleDocument;
import com.fleet.document.entity.VehicleDocumentAttribute;
import com.fleet.document.repository.VehicleDocumentAttributeRepository;
import com.fleet.document.service.normalization.AmountNormalizer;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VehicleCostReportServiceTest {

    @Mock
    private FleetVehicleClient fleetVehicleClient;

    @Mock
    private VehicleDocumentAttributeRepository attributeRepository;

    private VehicleCostReportService reportService;

    @BeforeEach
    void setUp() {
        reportService = new VehicleCostReportService(
                fleetVehicleClient,
                attributeRepository,
                new AmountNormalizer()
        );
    }

    @Test
    void exportsAllVisibleVehiclesWithSeparateCurrencyTotalsAndDocumentDetails() throws Exception {
        LocalDate today = LocalDate.now();
        VehicleBasicInfoResponse vehicleWithDocuments = vehicle(1L, 100L, "B-20-XYZ", "Dacia", "Duster", "ACTIVE");
        VehicleBasicInfoResponse vehicleWithoutDocuments = vehicle(2L, 100L, "A-10-ABC", "Ford", "Focus", "SOLD");
        when(fleetVehicleClient.visibleVehicles("Bearer token"))
                .thenReturn(List.of(vehicleWithDocuments, vehicleWithoutDocuments));

        VehicleDocumentAttribute invoice = attribute(
                1L,
                "EXPENSE_INVOICE",
                "UNKNOWN",
                null,
                Map.of(
                        "invoiceNumber", "INV-100",
                        "invoiceDate", "2026-01-15",
                        "totalAmount", new BigDecimal("120.50"),
                        "currency", "RON",
                        "expenseCategory", "SERVICE"
                ),
                "invoice.pdf",
                Instant.parse("2026-01-20T10:00:00Z")
        );
        VehicleDocumentAttribute roadTax = attribute(
                1L,
                "ROAD_TAX",
                "ROVINIETA",
                today.plusDays(10),
                Map.of(
                        "transactionId", "TX-200",
                        "validFrom", today.minusDays(5).toString(),
                        "amount", "50,25",
                        "currency", "eur"
                ),
                "road-tax.pdf",
                Instant.parse("2026-02-01T10:00:00Z")
        );
        VehicleDocumentAttribute unsupportedCurrency = attribute(
                1L,
                "EXPENSE_INVOICE",
                "UNKNOWN",
                null,
                Map.of("totalAmount", 10, "currency", "GBP"),
                "foreign-invoice.pdf",
                Instant.parse("2026-02-02T10:00:00Z")
        );
        when(attributeRepository.findByVehicleIdInAndStatusOrderByValidUntilAscCreatedAtDesc(
                List.of(2L, 1L),
                ApprovedDataStatus.ACTIVE
        )).thenReturn(List.of(invoice, roadTax, unsupportedCurrency));

        byte[] result = reportService.export("Bearer token", businessAdmin());

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(result))) {
            assertThat(workbook.getNumberOfSheets()).isEqualTo(2);
            assertThat(workbook.getSheetName(0)).isEqualTo("Vehicle Summary");
            assertThat(workbook.getSheetName(1)).isEqualTo("Document Details");

            Sheet summary = workbook.getSheet("Vehicle Summary");
            assertThat(summary.getLastRowNum()).isEqualTo(2);
            Row noDocuments = summary.getRow(1);
            assertThat(noDocuments.getCell(1).getStringCellValue()).isEqualTo("A-10-ABC");
            assertThat(noDocuments.getCell(5).getNumericCellValue()).isZero();
            assertThat(noDocuments.getCell(7).getNumericCellValue()).isZero();
            assertThat(noDocuments.getCell(10).getStringCellValue()).isEqualTo("No Expiration");

            Row withDocuments = summary.getRow(2);
            assertThat(withDocuments.getCell(1).getStringCellValue()).isEqualTo("B-20-XYZ");
            assertThat(withDocuments.getCell(5).getNumericCellValue()).isEqualTo(3);
            assertThat(withDocuments.getCell(6).getNumericCellValue()).isEqualTo(3);
            assertThat(withDocuments.getCell(7).getNumericCellValue()).isEqualTo(120.50);
            assertThat(withDocuments.getCell(8).getNumericCellValue()).isEqualTo(50.25);
            assertThat(withDocuments.getCell(9).getLocalDateTimeCellValue().toLocalDate()).isEqualTo(today.plusDays(10));
            assertThat(withDocuments.getCell(10).getStringCellValue()).isEqualTo("Expiring Soon");

            Sheet details = workbook.getSheet("Document Details");
            assertThat(findBusinessFileRow(details, "foreign-invoice.pdf").getCell(11).getLocalDateTimeCellValue().toLocalDate())
                    .isEqualTo(LocalDate.of(2026, 2, 2));
            assertThat(findDetailRow(details, "INV-100").getCell(9).getNumericCellValue()).isEqualTo(120.50);
            assertThat(findDetailRow(details, "INV-100").getCell(11).getLocalDateTimeCellValue().toLocalDate())
                    .isEqualTo(LocalDate.of(2026, 1, 15));
            assertThat(findDetailRow(details, "TX-200").getCell(14).getStringCellValue()).isEqualTo("Expiring Soon");
        }

        verify(attributeRepository).findByVehicleIdInAndStatusOrderByValidUntilAscCreatedAtDesc(
                List.of(2L, 1L),
                ApprovedDataStatus.ACTIVE
        );
    }

    @Test
    void usesExactExpirationBoundariesAndIncludesOrganizationForSuperadmin() throws Exception {
        LocalDate today = LocalDate.now();
        VehicleBasicInfoResponse vehicle = vehicle(5L, 200L, "CJ-01-AAA", "Volvo", "XC60", "INACTIVE");
        when(fleetVehicleClient.visibleVehicles("Bearer super")).thenReturn(List.of(vehicle));
        when(attributeRepository.findByVehicleIdInAndStatusOrderByValidUntilAscCreatedAtDesc(
                anyList(),
                org.mockito.ArgumentMatchers.eq(ApprovedDataStatus.ACTIVE)
        )).thenReturn(List.of(
                attribute(5L, "INSURANCE", "RCA", today.minusDays(1), Map.of(), "expired.pdf", Instant.now()),
                attribute(5L, "TECHNICAL_INSPECTION", "ITP", today, Map.of(), "today.pdf", Instant.now()),
                attribute(5L, "ROAD_TAX", "ROVINIETA", today.plusDays(30), Map.of(), "soon.pdf", Instant.now()),
                attribute(5L, "OTHER", "UNKNOWN", today.plusDays(31), Map.of(), "valid.pdf", Instant.now()),
                attribute(5L, "EXPENSE_INVOICE", "UNKNOWN", null, Map.of(), "no-expiry.pdf", Instant.now())
        ));

        byte[] result = reportService.export("Bearer super", superadmin());

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(result))) {
            Sheet summary = workbook.getSheet("Vehicle Summary");
            assertThat(summary.getRow(0).getCell(0).getStringCellValue()).isEqualTo("Organization ID");
            assertThat(summary.getRow(1).getCell(0).getNumericCellValue()).isEqualTo(200);
            assertThat(summary.getRow(1).getCell(11).getStringCellValue()).isEqualTo("Expired");

            Sheet details = workbook.getSheet("Document Details");
            assertThat(findFileRow(details, "expired.pdf").getCell(15).getStringCellValue()).isEqualTo("Expired");
            assertThat(findFileRow(details, "today.pdf").getCell(15).getStringCellValue()).isEqualTo("Expiring Soon");
            assertThat(findFileRow(details, "soon.pdf").getCell(15).getStringCellValue()).isEqualTo("Expiring Soon");
            assertThat(findFileRow(details, "valid.pdf").getCell(15).getStringCellValue()).isEqualTo("Valid");
            assertThat(findFileRow(details, "no-expiry.pdf").getCell(15).getStringCellValue()).isEqualTo("No Expiration");
        }
    }

    @Test
    void exportsAmountWithoutCurrencyWithoutAddingItToCurrencyTotals() throws Exception {
        VehicleBasicInfoResponse vehicle = vehicle(7L, 100L, "B-07-NOC", "Dacia", "Logan", "ACTIVE");
        when(fleetVehicleClient.visibleVehicles("Bearer token")).thenReturn(List.of(vehicle));
        when(attributeRepository.findByVehicleIdInAndStatusOrderByValidUntilAscCreatedAtDesc(
                List.of(7L),
                ApprovedDataStatus.ACTIVE
        )).thenReturn(List.of(attribute(
                7L,
                "EXPENSE_INVOICE",
                "UNKNOWN",
                null,
                Map.of("totalAmount", new BigDecimal("99.99")),
                "missing-currency.pdf",
                Instant.now()
        )));

        byte[] result = reportService.export("Bearer token", businessAdmin());

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(result))) {
            Row summary = workbook.getSheet("Vehicle Summary").getRow(1);
            assertThat(summary.getCell(7).getNumericCellValue()).isZero();
            assertThat(summary.getCell(8).getNumericCellValue()).isZero();
            assertThat(findBusinessFileRow(workbook.getSheet("Document Details"), "missing-currency.pdf")
                    .getCell(9).getNumericCellValue()).isEqualTo(99.99);
        }
    }

    @Test
    void employeeCannotExport() {
        assertThatThrownBy(() -> reportService.export("Bearer employee", employee()))
                .isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(fleetVehicleClient, attributeRepository);
    }

    private Row findDetailRow(Sheet sheet, String reference) {
        for (Row row : sheet) {
            if (row.getRowNum() > 0 && row.getCell(8) != null
                    && reference.equals(row.getCell(8).getStringCellValue())) {
                return row;
            }
        }
        throw new AssertionError("Detail row not found: " + reference);
    }

    private Row findFileRow(Sheet sheet, String fileName) {
        for (Row row : sheet) {
            if (row.getRowNum() > 0 && row.getCell(16) != null
                    && fileName.equals(row.getCell(16).getStringCellValue())) {
                return row;
            }
        }
        throw new AssertionError("File row not found: " + fileName);
    }

    private Row findBusinessFileRow(Sheet sheet, String fileName) {
        for (Row row : sheet) {
            if (row.getRowNum() > 0 && row.getCell(15) != null
                    && fileName.equals(row.getCell(15).getStringCellValue())) {
                return row;
            }
        }
        throw new AssertionError("File row not found: " + fileName);
    }

    private VehicleBasicInfoResponse vehicle(
            Long id,
            Long businessId,
            String licensePlate,
            String brand,
            String model,
            String status
    ) {
        return new VehicleBasicInfoResponse(id, businessId, licensePlate, brand, model, status, null, null);
    }

    private VehicleDocumentAttribute attribute(
            Long vehicleId,
            String documentType,
            String subtype,
            LocalDate validUntil,
            Map<String, Object> sourceData,
            String fileName,
            Instant uploadedAt
    ) {
        VehicleDocument document = VehicleDocument.builder()
                .id(UUID.randomUUID())
                .vehicleId(vehicleId)
                .originalFileName(fileName)
                .createdAt(uploadedAt)
                .build();
        return VehicleDocumentAttribute.builder()
                .id(UUID.randomUUID())
                .vehicleId(vehicleId)
                .document(document)
                .documentType(documentType)
                .subtype(subtype)
                .validFrom(sourceData.containsKey("validFrom")
                        ? LocalDate.parse(sourceData.get("validFrom").toString())
                        : null)
                .validUntil(validUntil)
                .sourceData(sourceData)
                .status(ApprovedDataStatus.ACTIVE)
                .build();
    }

    private Authentication businessAdmin() {
        return authentication("admin", 10L, 100L, "BUSINESS_ADMIN");
    }

    private Authentication superadmin() {
        return authentication("super", 1L, null, "SUPERADMIN");
    }

    private Authentication employee() {
        return authentication("employee", 20L, 100L, "EMPLOYEE");
    }

    private Authentication authentication(String username, Long userId, Long businessId, String role) {
        JwtPrincipal principal = new JwtPrincipal(username, userId, businessId, Set.of(role));
        return new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role))
        );
    }
}
