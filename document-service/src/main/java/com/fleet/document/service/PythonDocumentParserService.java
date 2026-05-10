package com.fleet.document.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fleet.document.config.DocumentParserProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class PythonDocumentParserService {

    private static final String DEFAULT_PARSER_NAME = "inspection-pdf-parser";
    private static final String DEFAULT_PARSER_VERSION = "1.0.0";

    private final DocumentParserProperties parserProperties;
    private final ObjectMapper objectMapper;

    public PythonParserResult parse(String pdfPath) {
        Process process;
        try {
            process = new ProcessBuilder(
                    parserProperties.getPythonExecutable(),
                    Path.of(parserProperties.getScriptPath()).toString(),
                    pdfPath
            ).start();
        } catch (IOException ex) {
            return failed("Could not start parser process: " + ex.getMessage());
        }

        // Drain stdout and stderr on separate threads to prevent pipe-buffer deadlock
        CompletableFuture<String> stdoutFuture = CompletableFuture.supplyAsync(() -> {
            try {
                return new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                return "";
            }
        });
        CompletableFuture<String> stderrFuture = CompletableFuture.supplyAsync(() -> {
            try {
                return new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                return "";
            }
        });

        try {
            boolean finished = process.waitFor(parserProperties.getTimeoutSeconds(), TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return failed("Parser timed out after " + parserProperties.getTimeoutSeconds() + " seconds");
            }

            String stdout = stdoutFuture.join().trim();
            String stderr = stderrFuture.join().trim();

            if (!StringUtils.hasText(stdout)) {
                return failed(StringUtils.hasText(stderr) ? stderr : "Parser produced no output");
            }

            JsonNode root = objectMapper.readTree(stdout);
            boolean parsed = "PARSED".equalsIgnoreCase(root.path("parserStatus").asText());
            if (!parsed || process.exitValue() != 0) {
                String errorMessage = root.path("errorMessage").asText(null);
                return failed(StringUtils.hasText(errorMessage) ? errorMessage : stderr);
            }

            JsonNode dataNode = root.path("data");
            Map<String, Object> data = dataNode.isMissingNode() || dataNode.isNull()
                    ? Map.of()
                    : objectMapper.convertValue(dataNode, new TypeReference<>() {
                    });

            return new PythonParserResult(
                    true,
                    valueOrDefault(root.path("parserName").asText(null), DEFAULT_PARSER_NAME),
                    valueOrDefault(root.path("parserVersion").asText(null), DEFAULT_PARSER_VERSION),
                    root.hasNonNull("confidence") ? root.get("confidence").decimalValue() : null,
                    data,
                    null
            );
        } catch (JsonProcessingException ex) {
            return failed("Parser returned invalid JSON: " + ex.getOriginalMessage());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
            return failed("Parser execution was interrupted");
        } catch (RuntimeException ex) {
            return failed("Could not read parser output: " + ex.getMessage());
        }
    }

    private PythonParserResult failed(String errorMessage) {
        return new PythonParserResult(
                false,
                DEFAULT_PARSER_NAME,
                DEFAULT_PARSER_VERSION,
                BigDecimal.ZERO,
                Map.of(),
                StringUtils.hasText(errorMessage) ? errorMessage : "Parser failed"
        );
    }

    private String valueOrDefault(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value : defaultValue;
    }
}
