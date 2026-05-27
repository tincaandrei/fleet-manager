package com.fleet.parser;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fleet.parser.config.InternalApiKeyProperties;
import com.fleet.parser.config.JwtProperties;
import com.fleet.parser.service.OllamaClient;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ParserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtProperties jwtProperties;

    @Autowired
    private InternalApiKeyProperties internalApiKeyProperties;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OllamaClient ollamaClient;

    @Test
    void healthIsPublic() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.service").value("parser-service"))
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void meRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void meReturnsJwtPrincipal() throws Exception {
        String token = Jwts.builder()
                .setSubject("admin")
                .claim("userId", 7L)
                .claim("roles", List.of("ADMIN", "STAFF"))
                .setExpiration(Date.from(Instant.now().plusSeconds(3600)))
                .signWith(
                        Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8)),
                        SignatureAlgorithm.HS256
                )
                .compact();

        mockMvc.perform(get("/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("admin"))
                .andExpect(jsonPath("$.userId").value(7))
                .andExpect(jsonPath("$.roles[*]", containsInAnyOrder("ADMIN", "STAFF")));
    }

    @Test
    void extractRequiresInternalApiKey() throws Exception {
        mockMvc.perform(multipart("/documents/extract")
                        .file(pdfFile())
                        .file(textPart("documentId", "00000000-0000-0000-0000-000000000001"))
                        .file(textPart("vehicleId", "1")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void extractRejectsNonPdfFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "notes.txt",
                "text/plain",
                "not a pdf".getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/documents/extract")
                        .file(file)
                        .file(textPart("documentId", "00000000-0000-0000-0000-000000000001"))
                        .file(textPart("vehicleId", "1"))
                        .header("X-Internal-Api-Key", internalApiKeyProperties.getApiKey()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.parserStatus").value("FAILED"))
                .andExpect(jsonPath("$.errorCode").value("UNSUPPORTED_FILE_TYPE"));
    }

    @Test
    void extractParsesPdfWithValidInternalApiKey() throws Exception {
        when(ollamaClient.extractJson(anyString())).thenReturn(objectMapper.readTree("""
                {
                  "policyNumber": "RO/123",
                  "insurerName": "Example Insurance",
                  "ownerName": "Example Owner",
                  "licensePlate": "B 123 XYZ",
                  "vin": "WVWZZZ1KZAW123456",
                  "validFrom": "2026-01-01",
                  "validUntil": "2027-01-01",
                  "llmConfidence": 0.9
                }
                """));

        mockMvc.perform(multipart("/documents/extract")
                        .file(pdfFile())
                        .file(textPart("documentId", "00000000-0000-0000-0000-000000000001"))
                        .file(textPart("vehicleId", "1"))
                        .file(textPart("declaredDocumentType", "RCA"))
                        .file(textPart("originalFileName", "insurance.pdf"))
                        .header("X-Internal-Api-Key", internalApiKeyProperties.getApiKey()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documentId").value("00000000-0000-0000-0000-000000000001"))
                .andExpect(jsonPath("$.parserStatus").value("PARSED"))
                .andExpect(jsonPath("$.detectedDocumentType").value("INSURANCE"))
                .andExpect(jsonPath("$.detectedSubtype").value("RCA"))
                .andExpect(jsonPath("$.extractionMethod").value("PDF_TEXT"))
                .andExpect(jsonPath("$.extractedData.licensePlate").value("B123XYZ"))
                .andExpect(jsonPath("$.extractedData.vin").value("WVWZZZ1KZAW123456"));
    }

    private MockMultipartFile pdfFile() throws Exception {
        return new MockMultipartFile("file", "insurance.pdf", "application/pdf", samplePdfBytes());
    }

    private MockMultipartFile textPart(String name, String value) {
        return new MockMultipartFile(name, "", "text/plain", value.getBytes(StandardCharsets.UTF_8));
    }

    private byte[] samplePdfBytes() throws Exception {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                content.beginText();
                content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                content.newLineAtOffset(50, 700);
                content.showText("Polita RCA asigurare auto numar RO/123 pentru B123XYZ VIN WVWZZZ1KZAW123456.");
                content.newLineAtOffset(0, -20);
                content.showText("Valabila de la 2026-01-01 pana la 2027-01-01. Societate asigurare Example Insurance.");
                content.endText();
            }
            document.save(output);
            return output.toByteArray();
        }
    }
}
