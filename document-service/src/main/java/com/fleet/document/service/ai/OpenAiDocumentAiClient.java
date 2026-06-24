package com.fleet.document.service.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fleet.document.dto.LlmUsageDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Service
public class OpenAiDocumentAiClient implements DocumentAiClient {

    private static final String PROVIDER = "openai";

    private final ObjectMapper objectMapper;
    private final RestClient openAiClient;
    private final String model;
    private final int maxOutputTokens;
    private final double temperature;

    @Autowired
    public OpenAiDocumentAiClient(
            ObjectMapper objectMapper,
            @Value("${openai.base-url}") String baseUrl,
            @Value("${openai.api-key}") String apiKey,
            @Value("${openai.model}") String model,
            @Value("${openai.timeout}") Duration timeout,
            @Value("${openai.max-output-tokens}") int maxOutputTokens,
            @Value("${openai.temperature}") double temperature
    ) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("OPENAI_API_KEY must be configured for document parsing");
        }
        this.objectMapper = objectMapper;
        this.openAiClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey.trim())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .requestFactory(requestFactory(timeout))
                .build();
        this.model = model;
        this.maxOutputTokens = maxOutputTokens;
        this.temperature = temperature;
    }

    OpenAiDocumentAiClient(
            ObjectMapper objectMapper,
            RestClient openAiClient,
            String model,
            int maxOutputTokens,
            double temperature
    ) {
        this.objectMapper = objectMapper;
        this.openAiClient = openAiClient;
        this.model = model;
        this.maxOutputTokens = maxOutputTokens;
        this.temperature = temperature;
    }

    @Override
    public AiExtractionResponse extract(AiExtractionRequest request) {
        ObjectNode apiRequest = buildRequest(request);

        try {
            JsonNode response = openAiClient.post()
                    .uri("/responses")
                    .body(apiRequest)
                    .retrieve()
                    .body(JsonNode.class);
            if (response == null || response.isNull()) {
                throw new DocumentAiException("OPENAI_INVALID_RESPONSE", "OpenAI returned an empty response");
            }
            return new AiExtractionResponse(parseJsonContent(extractOutputText(response)), usageFrom(response));
        } catch (ResourceAccessException exception) {
            if (isTimeout(exception)) {
                throw new DocumentAiException("OPENAI_TIMEOUT", "OpenAI request timed out", exception);
            }
            throw new DocumentAiException("OPENAI_UNAVAILABLE", "OpenAI is unavailable", exception);
        } catch (RestClientResponseException exception) {
            throw mapHttpException(exception);
        } catch (DocumentAiException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw new DocumentAiException("OPENAI_UNAVAILABLE", "OpenAI request failed", exception);
        }
    }

    private ObjectNode buildRequest(AiExtractionRequest request) {
        ObjectNode apiRequest = objectMapper.createObjectNode();
        apiRequest.put("model", model);
        apiRequest.set("input", input(request));
        apiRequest.set("text", textFormat(request.jsonSchema()));
        apiRequest.put("max_output_tokens", maxOutputTokens);
        apiRequest.put("temperature", temperature);
        apiRequest.put("store", false);
        return apiRequest;
    }

    private ArrayNode input(AiExtractionRequest request) {
        ArrayNode input = objectMapper.createArrayNode();
        input.add(message("system", """
                You are a strict JSON extraction engine for Romanian vehicle fleet documents.
                Return only values explicitly visible in the provided document text.
                Never invent values. Use null for missing or uncertain values.
                """));
        input.add(message("user", """
                Document type: %s
                Document subtype: %s
                Extraction method: %s

                Extraction instructions:
                %s

                Document text:
                %s
                """.formatted(
                request.documentType(),
                request.subtype(),
                request.extractionMethod(),
                request.promptInstructions(),
                limitText(request.extractedText())
        )));
        return input;
    }

    private ObjectNode message(String role, String content) {
        ObjectNode message = objectMapper.createObjectNode();
        message.put("role", role);
        message.put("content", content);
        return message;
    }

    private ObjectNode textFormat(ObjectNode jsonSchema) {
        ObjectNode text = objectMapper.createObjectNode();
        ObjectNode format = objectMapper.createObjectNode();
        format.put("type", "json_schema");
        format.put("name", "vehicle_document_extraction");
        format.put("strict", true);
        format.set("schema", jsonSchema);
        text.set("format", format);
        return text;
    }

    private String extractOutputText(JsonNode response) {
        if (response.hasNonNull("output_text")) {
            return response.get("output_text").asText();
        }

        JsonNode output = response.path("output");
        if (!output.isArray()) {
            throw new DocumentAiException("OPENAI_INVALID_RESPONSE", "OpenAI response does not contain output text");
        }

        List<String> chunks = new ArrayList<>();
        for (JsonNode item : output) {
            JsonNode content = item.path("content");
            if (!content.isArray()) {
                continue;
            }
            for (JsonNode contentItem : content) {
                if (contentItem.hasNonNull("refusal")) {
                    throw new DocumentAiException("OPENAI_INVALID_RESPONSE", "OpenAI refused the document extraction request");
                }
                if ("output_text".equals(contentItem.path("type").asText()) && contentItem.hasNonNull("text")) {
                    chunks.add(contentItem.get("text").asText());
                }
            }
        }

        String text = String.join("", chunks).trim();
        if (text.isBlank()) {
            throw new DocumentAiException("OPENAI_INVALID_RESPONSE", "OpenAI response does not contain output text");
        }
        return text;
    }

    private JsonNode parseJsonContent(String content) {
        try {
            return objectMapper.readTree(content);
        } catch (JsonProcessingException exception) {
            throw new DocumentAiException("OPENAI_INVALID_RESPONSE", "OpenAI returned invalid JSON", exception);
        }
    }

    private LlmUsageDto usageFrom(JsonNode response) {
        JsonNode usage = response.path("usage");
        return new LlmUsageDto(
                PROVIDER,
                response.path("model").asText(model),
                response.path("id").asText(null),
                integerOrNull(usage.path("input_tokens")),
                integerOrNull(usage.path("output_tokens")),
                integerOrNull(usage.path("total_tokens"))
        );
    }

    private Integer integerOrNull(JsonNode node) {
        return node != null && node.isInt() ? node.asInt() : null;
    }

    private DocumentAiException mapHttpException(RestClientResponseException exception) {
        int status = exception.getStatusCode().value();
        if (status == 401 || status == 403) {
            return new DocumentAiException("OPENAI_AUTH_FAILED", "OpenAI API key was rejected", exception);
        }
        if (status == 429) {
            return new DocumentAiException("OPENAI_RATE_LIMITED", "OpenAI rate limit was reached", exception);
        }
        if (status >= 500) {
            return new DocumentAiException("OPENAI_UNAVAILABLE", "OpenAI service is unavailable", exception);
        }
        return new DocumentAiException("OPENAI_INVALID_RESPONSE", "OpenAI request was rejected", exception);
    }

    private boolean isTimeout(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof SocketTimeoutException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private SimpleClientHttpRequestFactory requestFactory(Duration timeout) {
        int timeoutMillis = (int) Math.min(Integer.MAX_VALUE, Math.max(1, timeout.toMillis()));
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeoutMillis);
        factory.setReadTimeout(timeoutMillis);
        return factory;
    }

    private String limitText(String text) {
        if (text == null) {
            return "";
        }
        int maxCharacters = 12_000;
        if (text.length() <= maxCharacters) {
            return text;
        }
        return text.substring(0, maxCharacters) + "\n[TRUNCATED]";
    }
}
