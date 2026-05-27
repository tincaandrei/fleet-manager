package com.fleet.parser.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fleet.parser.config.OllamaProperties;
import com.fleet.parser.dto.OllamaChatRequest;
import com.fleet.parser.dto.OllamaChatResponse;
import com.fleet.parser.dto.ParserErrorCode;
import com.fleet.parser.exception.OllamaClientException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.SocketTimeoutException;
import java.util.List;

@Service
public class OllamaClient {

    private final RestClient restClient;
    private final OllamaProperties properties;
    private final ObjectMapper objectMapper;

    public OllamaClient(RestClient ollamaRestClient, OllamaProperties properties, ObjectMapper objectMapper) {
        this.restClient = ollamaRestClient;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public JsonNode extractJson(String prompt) {
        OllamaChatRequest request = new OllamaChatRequest(
                properties.getModel(),
                false,
                "json",
                List.of(
                        new OllamaChatRequest.Message("system", systemPrompt()),
                        new OllamaChatRequest.Message("user", prompt)
                )
        );

        try {
            OllamaChatResponse response = restClient.post()
                    .uri("/api/chat")
                    .body(request)
                    .retrieve()
                    .body(OllamaChatResponse.class);

            if (response == null || response.message() == null || response.message().content() == null) {
                throw new OllamaClientException(ParserErrorCode.OLLAMA_INVALID_RESPONSE, "Ollama returned an empty response");
            }
            return parseJsonContent(response.message().content());
        } catch (OllamaClientException exception) {
            throw exception;
        } catch (ResourceAccessException exception) {
            if (isTimeout(exception)) {
                throw new OllamaClientException(ParserErrorCode.OLLAMA_TIMEOUT, "Ollama request timed out", exception);
            }
            throw new OllamaClientException(ParserErrorCode.OLLAMA_UNAVAILABLE, "Ollama is unavailable", exception);
        } catch (RestClientException exception) {
            throw new OllamaClientException(ParserErrorCode.OLLAMA_UNAVAILABLE, "Ollama request failed", exception);
        }
    }

    private JsonNode parseJsonContent(String content) {
        String candidate = stripMarkdownFence(content.trim());
        int firstBrace = candidate.indexOf('{');
        int lastBrace = candidate.lastIndexOf('}');
        if (firstBrace >= 0 && lastBrace > firstBrace) {
            candidate = candidate.substring(firstBrace, lastBrace + 1);
        }

        try {
            return objectMapper.readTree(candidate);
        } catch (JsonProcessingException exception) {
            throw new OllamaClientException(ParserErrorCode.OLLAMA_INVALID_RESPONSE, "Ollama returned invalid JSON", exception);
        }
    }

    private String stripMarkdownFence(String content) {
        if (content.startsWith("```")) {
            return content.replaceFirst("^```(?:json)?\\s*", "")
                    .replaceFirst("\\s*```$", "")
                    .trim();
        }
        return content;
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

    private String systemPrompt() {
        return """
                You are a strict JSON extraction engine for vehicle document parsing.
                Return only JSON. Never invent values. Use null for missing or uncertain fields.
                """;
    }
}
