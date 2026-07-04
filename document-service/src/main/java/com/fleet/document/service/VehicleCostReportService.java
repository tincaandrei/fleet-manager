package com.fleet.document.service;

import com.fleet.document.dto.VehicleBasicInfoResponse;
import com.fleet.document.entity.ApprovedDataStatus;
import com.fleet.document.entity.VehicleDocument;
import com.fleet.document.entity.VehicleDocumentAttribute;
import com.fleet.document.repository.VehicleDocumentAttributeRepository;
import com.fleet.document.service.normalization.AmountNormalizer;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VehicleCostReportService {

    private static final List<String> CURRENCIES = List.of("RON", "EUR");
    private static final Set<String> SUPPORTED_CURRENCIES = Set.copyOf(CURRENCIES);
    private static final int EXPIRING_SOON_DAYS = 30;
    private static final int MAX_COLUMN_WIDTH = 60 * 256;

    private final FleetVehicleClient fleetVehicleClient;
    private final VehicleDocumentAttributeRepository attributeRepository;
    private final AmountNormalizer amountNormalizer;

    @Transactional(readOnly = true)
    public byte[] export(String authorizationHeader, Authentication authentication) {
        if (!SecurityUtils.canReview(authentication)) {
            throw new AccessDeniedException("Access denied");
        }

        boolean includeBusiness = SecurityUtils.isSuperadmin(authentication);
        LocalDate reportDate = LocalDate.now();
        List<VehicleBasicInfoResponse> vehicles = fleetVehicleClient.visibleVehicles(authorizationHeader).stream()
                .sorted(vehicleComparator())
                .toList();
        List<Long> vehicleIds = vehicles.stream().map(VehicleBasicInfoResponse::id).toList();
        Map<Long, List<ReportDocument>> documentsByVehicle = loadDocuments(vehicleIds);

        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            WorkbookStyles styles = createStyles(workbook);
            writeSummarySheet(workbook, styles, vehicles, documentsByVehicle, includeBusiness, reportDate);
            writeDetailsSheet(workbook, styles, vehicles, documentsByVehicle, includeBusiness, reportDate);
            workbook.write(output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Could not generate vehicle cost report", exception);
        }
    }

    private Map<Long, List<ReportDocument>> loadDocuments(List<Long> vehicleIds) {
        if (vehicleIds.isEmpty()) {
            return Map.of();
        }
        return attributeRepository
                .findByVehicleIdInAndStatusOrderByValidUntilAscCreatedAtDesc(vehicleIds, ApprovedDataStatus.ACTIVE)
                .stream()
                .map(this::toReportDocument)
                .collect(Collectors.groupingBy(
                        ReportDocument::vehicleId,
                        LinkedHashMap::new,
                        Collectors.collectingAndThen(Collectors.toList(), items -> items.stream()
                                .sorted(documentComparator())
                                .toList())
                ));
    }

    private ReportDocument toReportDocument(VehicleDocumentAttribute attribute) {
        Map<String, Object> data = attribute.getSourceData() == null ? Map.of() : attribute.getSourceData();
        VehicleDocument document = attribute.getDocument();
        BigDecimal amount = firstAmount(data, "totalAmount", "amount");
        String currency = upperText(data, "currency");
        return new ReportDocument(
                attribute.getVehicleId(),
                document.getId(),
                document.getOriginalFileName(),
                attribute.getDocumentType(),
                attribute.getSubtype(),
                text(data, "expenseCategory"),
                firstText(data, "invoiceNumber", "transactionId", "policyNumber", "inspectionNumber", "documentTitle"),
                amount,
                currency,
                firstDate(data, "invoiceDate", "inspectionDate", "issueDate", "validFrom"),
                attribute.getValidFrom(),
                attribute.getValidUntil(),
                document.getCreatedAt()
        );
    }

    private void writeSummarySheet(
            Workbook workbook,
            WorkbookStyles styles,
            List<VehicleBasicInfoResponse> vehicles,
            Map<Long, List<ReportDocument>> documentsByVehicle,
            boolean includeBusiness,
            LocalDate reportDate
    ) {
        Sheet sheet = workbook.createSheet("Vehicle Summary");
        List<String> headers = new ArrayList<>();
        if (includeBusiness) {
            headers.add("Organization ID");
        }
        headers.addAll(List.of(
                "Vehicle ID", "License Plate", "Brand", "Model", "Vehicle Status",
                "Document Count", "Cost Entry Count", "Total RON", "Total EUR",
                "Next Expiration", "Overall Expiration Status"
        ));
        writeHeader(sheet, headers, styles.header());

        int rowIndex = 1;
        for (VehicleBasicInfoResponse vehicle : vehicles) {
            List<ReportDocument> documents = documentsByVehicle.getOrDefault(vehicle.id(), List.of());
            Map<String, BigDecimal> totals = totals(documents);
            Row row = sheet.createRow(rowIndex++);
            int column = 0;
            if (includeBusiness) {
                setNumber(row, column++, vehicle.businessId(), styles.integer());
            }
            setNumber(row, column++, vehicle.id(), styles.integer());
            setText(row, column++, vehicle.licensePlate());
            setText(row, column++, vehicle.brand());
            setText(row, column++, vehicle.model());
            setText(row, column++, vehicle.status());
            setNumber(row, column++, documents.size(), styles.integer());
            setNumber(row, column++, documents.stream().filter(item -> item.amount() != null).count(), styles.integer());
            for (String currency : CURRENCIES) {
                setDecimal(row, column++, totals.get(currency), styles.money());
            }
            setDate(row, column++, nextExpiration(documents, reportDate), styles.date());
            setText(row, column, overallExpirationStatus(documents, reportDate));
        }

        finishSheet(sheet, headers.size(), rowIndex);
    }

    private void writeDetailsSheet(
            Workbook workbook,
            WorkbookStyles styles,
            List<VehicleBasicInfoResponse> vehicles,
            Map<Long, List<ReportDocument>> documentsByVehicle,
            boolean includeBusiness,
            LocalDate reportDate
    ) {
        Sheet sheet = workbook.createSheet("Document Details");
        List<String> headers = new ArrayList<>();
        if (includeBusiness) {
            headers.add("Organization ID");
        }
        headers.addAll(List.of(
                "Vehicle ID", "License Plate", "Brand", "Model", "Vehicle Status",
                "Document Type", "Subtype", "Category", "Document Reference",
                "Amount", "Currency", "Document Date", "Valid From", "Valid Until",
                "Expiration Status", "File Name", "Document ID"
        ));
        writeHeader(sheet, headers, styles.header());

        int rowIndex = 1;
        for (VehicleBasicInfoResponse vehicle : vehicles) {
            List<ReportDocument> documents = documentsByVehicle.getOrDefault(vehicle.id(), List.of());
            if (documents.isEmpty()) {
                continue;
            }
            for (ReportDocument document : documents) {
                Row row = sheet.createRow(rowIndex++);
                int column = writeVehicleColumns(row, vehicle, includeBusiness, styles);
                setText(row, column++, document.documentType());
                setText(row, column++, document.subtype());
                setText(row, column++, document.category());
                setText(row, column++, document.reference());
                setDecimal(row, column++, document.amount(), styles.money());
                setText(row, column++, document.currency());
                setDate(row, column++, documentDate(document), styles.date());
                setDate(row, column++, document.validFrom(), styles.date());
                setDate(row, column++, document.validUntil(), styles.date());
                setText(row, column++, expirationStatus(document.validUntil(), reportDate));
                setText(row, column++, document.fileName());
                setText(row, column, document.documentId().toString());
            }

            Map<String, BigDecimal> totals = totals(documents);
            for (String currency : CURRENCIES) {
                Row subtotal = sheet.createRow(rowIndex++);
                int column = writeVehicleColumns(subtotal, vehicle, includeBusiness, styles);
                Cell labelCell = subtotal.createCell(column);
                labelCell.setCellValue("Vehicle Subtotal");
                labelCell.setCellStyle(styles.subtotal());
                for (int i = column + 1; i < column + 4; i++) {
                    subtotal.createCell(i).setCellStyle(styles.subtotal());
                }
                setDecimal(subtotal, column + 4, totals.get(currency), styles.subtotalMoney());
                Cell currencyCell = subtotal.createCell(column + 5);
                currencyCell.setCellValue(currency);
                currencyCell.setCellStyle(styles.subtotal());
            }
        }

        finishSheet(sheet, headers.size(), rowIndex);
    }

    private int writeVehicleColumns(
            Row row,
            VehicleBasicInfoResponse vehicle,
            boolean includeBusiness,
            WorkbookStyles styles
    ) {
        int column = 0;
        if (includeBusiness) {
            setNumber(row, column++, vehicle.businessId(), styles.integer());
        }
        setNumber(row, column++, vehicle.id(), styles.integer());
        setText(row, column++, vehicle.licensePlate());
        setText(row, column++, vehicle.brand());
        setText(row, column++, vehicle.model());
        setText(row, column++, vehicle.status());
        return column;
    }

    private WorkbookStyles createStyles(Workbook workbook) {
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setColor(IndexedColors.WHITE.getIndex());
        CellStyle header = workbook.createCellStyle();
        header.setFont(headerFont);
        header.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        header.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        header.setAlignment(HorizontalAlignment.CENTER);

        CellStyle date = workbook.createCellStyle();
        date.setDataFormat(workbook.getCreationHelper().createDataFormat().getFormat("yyyy-mm-dd"));

        CellStyle money = workbook.createCellStyle();
        money.setDataFormat(workbook.getCreationHelper().createDataFormat().getFormat("#,##0.00"));

        CellStyle integer = workbook.createCellStyle();
        integer.setDataFormat(workbook.getCreationHelper().createDataFormat().getFormat("0"));

        Font subtotalFont = workbook.createFont();
        subtotalFont.setBold(true);
        CellStyle subtotal = workbook.createCellStyle();
        subtotal.setFont(subtotalFont);
        subtotal.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
        subtotal.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        CellStyle subtotalMoney = workbook.createCellStyle();
        subtotalMoney.cloneStyleFrom(subtotal);
        subtotalMoney.setDataFormat(workbook.getCreationHelper().createDataFormat().getFormat("#,##0.00"));
        return new WorkbookStyles(header, date, money, integer, subtotal, subtotalMoney);
    }

    private void writeHeader(Sheet sheet, List<String> headers, CellStyle style) {
        Row row = sheet.createRow(0);
        row.setHeightInPoints(24);
        for (int i = 0; i < headers.size(); i++) {
            Cell cell = row.createCell(i);
            cell.setCellValue(headers.get(i));
            cell.setCellStyle(style);
        }
    }

    private void finishSheet(Sheet sheet, int columnCount, int nextRowIndex) {
        sheet.createFreezePane(0, 1);
        sheet.setAutoFilter(new CellRangeAddress(0, Math.max(0, nextRowIndex - 1), 0, columnCount - 1));
        for (int column = 0; column < columnCount; column++) {
            sheet.autoSizeColumn(column);
            sheet.setColumnWidth(column, Math.min(sheet.getColumnWidth(column) + 512, MAX_COLUMN_WIDTH));
        }
    }

    private Map<String, BigDecimal> totals(List<ReportDocument> documents) {
        Map<String, BigDecimal> result = CURRENCIES.stream()
                .collect(Collectors.toMap(Function.identity(), ignored -> BigDecimal.ZERO, (left, right) -> left, LinkedHashMap::new));
        for (ReportDocument document : documents) {
            if (document.amount() != null
                    && document.currency() != null
                    && SUPPORTED_CURRENCIES.contains(document.currency())) {
                result.merge(document.currency(), document.amount(), BigDecimal::add);
            }
        }
        return result;
    }

    private LocalDate nextExpiration(List<ReportDocument> documents, LocalDate reportDate) {
        return documents.stream()
                .map(ReportDocument::validUntil)
                .filter(date -> date != null && !date.isBefore(reportDate))
                .min(LocalDate::compareTo)
                .orElse(null);
    }

    private String overallExpirationStatus(List<ReportDocument> documents, LocalDate reportDate) {
        if (documents.stream().map(ReportDocument::validUntil).filter(date -> date != null)
                .anyMatch(date -> date.isBefore(reportDate))) {
            return "Expired";
        }
        LocalDate soonBoundary = reportDate.plusDays(EXPIRING_SOON_DAYS);
        if (documents.stream().map(ReportDocument::validUntil).filter(date -> date != null)
                .anyMatch(date -> !date.isAfter(soonBoundary))) {
            return "Expiring Soon";
        }
        if (documents.stream().anyMatch(document -> document.validUntil() != null)) {
            return "Valid";
        }
        return "No Expiration";
    }

    private String expirationStatus(LocalDate validUntil, LocalDate reportDate) {
        if (validUntil == null) {
            return "No Expiration";
        }
        if (validUntil.isBefore(reportDate)) {
            return "Expired";
        }
        if (!validUntil.isAfter(reportDate.plusDays(EXPIRING_SOON_DAYS))) {
            return "Expiring Soon";
        }
        return "Valid";
    }

    private LocalDate documentDate(ReportDocument document) {
        if (document.documentDate() != null) {
            return document.documentDate();
        }
        Instant uploadedAt = document.uploadedAt();
        return uploadedAt == null ? null : uploadedAt.atZone(ZoneId.systemDefault()).toLocalDate();
    }

    private BigDecimal firstAmount(Map<String, Object> data, String... keys) {
        for (String key : keys) {
            Object value = data.get(key);
            if (value instanceof BigDecimal decimal) {
                return decimal;
            }
            if (value instanceof Number number) {
                try {
                    return new BigDecimal(number.toString());
                } catch (NumberFormatException ignored) {
                    // Try the next supported amount field.
                }
            }
            if (value != null) {
                BigDecimal normalized = amountNormalizer.normalize(value.toString());
                if (normalized != null) {
                    return normalized;
                }
            }
        }
        return null;
    }

    private LocalDate firstDate(Map<String, Object> data, String... keys) {
        for (String key : keys) {
            Object value = data.get(key);
            if (value instanceof LocalDate date) {
                return date;
            }
            if (value != null) {
                try {
                    return LocalDate.parse(value.toString().trim());
                } catch (DateTimeParseException ignored) {
                    // Try the next supported date field.
                }
            }
        }
        return null;
    }

    private String firstText(Map<String, Object> data, String... keys) {
        for (String key : keys) {
            String value = text(data, key);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private String text(Map<String, Object> data, String key) {
        Object value = data.get(key);
        return value == null || !StringUtils.hasText(value.toString()) ? null : value.toString().trim();
    }

    private String upperText(Map<String, Object> data, String key) {
        String value = text(data, key);
        return value == null ? null : value.toUpperCase(Locale.ROOT);
    }

    private Comparator<VehicleBasicInfoResponse> vehicleComparator() {
        return Comparator
                .comparing(VehicleBasicInfoResponse::businessId, Comparator.nullsFirst(Long::compareTo))
                .thenComparing(vehicle -> safe(vehicle.licensePlate()), String.CASE_INSENSITIVE_ORDER)
                .thenComparing(VehicleBasicInfoResponse::id);
    }

    private Comparator<ReportDocument> documentComparator() {
        return Comparator
                .comparing(this::documentDate, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(document -> safe(document.documentType()), String.CASE_INSENSITIVE_ORDER)
                .thenComparing(ReportDocument::documentId);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private void setText(Row row, int column, Object value) {
        row.createCell(column).setCellValue(value == null ? "" : value.toString());
    }

    private void setNumber(Row row, int column, Number value, CellStyle style) {
        Cell cell = row.createCell(column);
        if (value != null) {
            cell.setCellValue(value.doubleValue());
        }
        cell.setCellStyle(style);
    }

    private void setDecimal(Row row, int column, BigDecimal value, CellStyle style) {
        Cell cell = row.createCell(column);
        if (value != null) {
            cell.setCellValue(value.doubleValue());
        }
        cell.setCellStyle(style);
    }

    private void setDate(Row row, int column, LocalDate value, CellStyle style) {
        Cell cell = row.createCell(column);
        if (value != null) {
            cell.setCellValue(value);
        }
        cell.setCellStyle(style);
    }

    private record ReportDocument(
            Long vehicleId,
            UUID documentId,
            String fileName,
            String documentType,
            String subtype,
            String category,
            String reference,
            BigDecimal amount,
            String currency,
            LocalDate documentDate,
            LocalDate validFrom,
            LocalDate validUntil,
            Instant uploadedAt
    ) {
    }

    private record WorkbookStyles(
            CellStyle header,
            CellStyle date,
            CellStyle money,
            CellStyle integer,
            CellStyle subtotal,
            CellStyle subtotalMoney
    ) {
    }
}
